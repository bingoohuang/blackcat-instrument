package com.github.bingoohuang.blackcat.javaagent.instrument;

import com.github.bingoohuang.blackcat.javaagent.callback.Blackcat;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatJavaAgentCallback;
import com.github.bingoohuang.blackcat.javaagent.callback.BlackcatJavaAgentInterceptor;
import com.github.bingoohuang.blackcat.javaagent.utils.Asms;
import com.github.bingoohuang.blackcat.javaagent.utils.Debugs;
import com.github.bingoohuang.blackcat.javaagent.utils.Helper;
import com.github.bingoohuang.blackcat.javaagent.utils.Tuple;
import lombok.val;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;

import static com.github.bingoohuang.blackcat.javaagent.utils.Asms.*;
import static com.github.bingoohuang.blackcat.javaagent.utils.TreeAsms.*;
import static org.objectweb.asm.Opcodes.*;

public class BlackcatInstrument {
    protected final BlackcatJavaAgentInterceptor interceptor = BlackcatJavaAgentCallback.INSTANCE;
    protected final String className;
    protected final byte[] classFileBuffer;
    protected ClassNode classNode;
    protected Type classType;
    protected MethodNode methodNode;
    protected Type[] methodArgs;
    protected Type methodReturnType;
    protected int methodOffset;
    protected LabelNode startNode;
    protected int catVarIndex;

    public BlackcatInstrument(byte[] classFileBuffer) {
        this.classFileBuffer = classFileBuffer;

        classNode = new ClassNode();
        val cr = new ClassReader(classFileBuffer);
        cr.accept(classNode, 0);
        className = c(classNode.name);
        classType = Type.getType("L" + classNode.name + ";");
    }

    public Tuple<Boolean, byte[]> modifyClass() {
        val ok = interceptor.interceptClass(classNode, className);
        if (!ok) return new Tuple(false, classFileBuffer);

        int count = modifyMethodCount(classNode.methods);
        if (count == 0) return new Tuple(false, classFileBuffer);

        val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);

        val bytes = cw.toByteArray();

        Debugs.writeClassFile(classNode, className, bytes);

        return new Tuple(true, bytes);
    }

    private int modifyMethodCount(List<MethodNode> methods) {
        int transformedCount = 0;
        for (MethodNode node : methods) {
            if (modifyMethod(node)) ++transformedCount;
        }

        return transformedCount;
    }

    private boolean modifyMethod(MethodNode mn) {
        if (Helper.isAbstract(mn)) return false;
        if (!interceptor.interceptMethod(classNode, mn)) return false;

        methodNode = mn;
        methodArgs = Type.getArgumentTypes(methodNode.desc);
        methodReturnType = Type.getReturnType(methodNode.desc);
        methodOffset = Helper.isStatic(methodNode) ? 0 : 1;

        addTraceStart();
        addTraceReturn();
        addTraceThrow();
        addTraceThrowableUncaught();

        return true;
    }

    private void addTraceStart() {
        val insnList = new InsnList();

        addGetCallback(insnList);

        insnList.add(new VarInsnNode(ALOAD, catVarIndex));
        insnList.add(new LdcInsnNode(className));
        insnList.add(new LdcInsnNode(methodNode.name));
        insnList.add(new LdcInsnNode(Asms.describeMethod(methodNode, false)));
        insnList.add(getPushInst(methodArgs.length));
        insnList.add(new TypeInsnNode(ANEWARRAY, p(Object.class)));
        for (int i = 0; i < methodArgs.length; i++) {
            insnList.add(new InsnNode(DUP));
            insnList.add(getPushInst(i));
            insnList.add(getLoadInst(methodArgs[i], getArgumentPosition(i)));
            MethodInsnNode mNode = getWrapperCtorInst(methodArgs[i]);
            if (mNode != null) insnList.add(mNode);
            insnList.add(new InsnNode(AASTORE));
        }

        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(Blackcat.class), "start",
                sig(void.class, String.class, String.class, String.class, Object[].class),
                false));

        startNode = new LabelNode();
        methodNode.instructions.insert(startNode);
        methodNode.instructions.insert(insnList);
    }

    private void addGetCallback(InsnList insnList) {
        insnList.add(new TypeInsnNode(NEW, p(Blackcat.class)));
        insnList.add(new InsnNode(DUP));
        insnList.add(new MethodInsnNode(INVOKESPECIAL,
                p(Blackcat.class), "<init>", sig(void.class), false));
        catVarIndex = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, catVarIndex));
        methodNode.maxLocals++;
    }

    private void addTraceReturn() {
        val insnList = methodNode.instructions;

        val it = insnList.iterator();
        while (it.hasNext()) {
            val insnNode = (AbstractInsnNode) it.next();

            switch (insnNode.getOpcode()) {
                case RETURN:
                    insnList.insertBefore(insnNode, getVoidReturnTraceInsts());
                    break;
                case IRETURN:
                case LRETURN:
                case FRETURN:
                case ARETURN:
                case DRETURN:
                    insnList.insertBefore(insnNode, getReturnTraceInsts());
            }
        }
    }

    private void addTraceThrow() {
        val it = methodNode.instructions.iterator();
        while (it.hasNext()) {
            val insnNode = (AbstractInsnNode) it.next();

            switch (insnNode.getOpcode()) {
                case ATHROW:
                    methodNode.instructions.insertBefore(insnNode, getThrowTraceInsts());
                    break;
            }
        }
    }

    private void addTraceThrowableUncaught() {
        val insnList = methodNode.instructions;

        LabelNode endNode = new LabelNode();
        insnList.add(endNode);

        addCatchBlock(startNode, endNode);

    }

    private void addCatchBlock(LabelNode startNode, LabelNode endNode) {
        val insnList = new InsnList();

        val handlerNode = new LabelNode();
        insnList.add(handlerNode);

        int exceptionVariablePosition = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, exceptionVariablePosition));
        methodOffset++;

        insnList.add(new VarInsnNode(ALOAD, catVarIndex));
        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(Blackcat.class), "uncaught",
                sig(void.class, Throwable.class), false));

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new InsnNode(ATHROW));

        methodNode.tryCatchBlocks.add(new TryCatchBlockNode(
                startNode, endNode, handlerNode, "java/lang/Throwable"));
        methodNode.instructions.add(insnList);
    }

    private InsnList getVoidReturnTraceInsts() {
        val insnList = new InsnList();
        insnList.add(new VarInsnNode(ALOAD, catVarIndex));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(Blackcat.class), "finish",
                sig(void.class), false));

        return insnList;
    }

    private InsnList getThrowTraceInsts() {
        val insnList = new InsnList();

        int exceptionVariablePosition = getFistAvailablePosition();
        insnList.add(new VarInsnNode(ASTORE, exceptionVariablePosition));

        methodOffset++;

        insnList.add(new VarInsnNode(ALOAD, catVarIndex));
        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(Blackcat.class), "caught",
                sig(void.class, Throwable.class), false));

        insnList.add(new VarInsnNode(ALOAD, exceptionVariablePosition));

        return insnList;
    }

    private InsnList getReturnTraceInsts() {
        val insnList = new InsnList();

        int returnedVariablePosition = getFistAvailablePosition();
        insnList.add(getStoreInst(methodReturnType, returnedVariablePosition));

        updateMethodOffset(methodReturnType);
        insnList.add(new VarInsnNode(ALOAD, catVarIndex));
        insnList.add(getLoadInst(methodReturnType, returnedVariablePosition));
        MethodInsnNode mNode = getWrapperCtorInst(methodReturnType);
        if (mNode != null) insnList.add(mNode);
        insnList.add(new MethodInsnNode(INVOKEVIRTUAL,
                p(Blackcat.class), "finish",
                sig(void.class, Object.class), false));

        insnList.add(getLoadInst(methodReturnType, returnedVariablePosition));

        return insnList;
    }

    private int getFistAvailablePosition() {
        return methodNode.maxLocals + methodOffset;
    }

    protected void updateMethodOffset(Type type) {
        char charType = type.getDescriptor().charAt(0);
        methodOffset += (charType == 'J' || charType == 'D') ? 2 : 1;
    }

    public int getArgumentPosition(int argNo) {
        return Helper.getArgPosition(methodOffset, methodArgs, argNo);
    }

}
