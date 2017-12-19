package com.github.bingoohuang.blackcat.instrument.utils;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static com.github.bingoohuang.blackcat.instrument.utils.Asms.ci;
import static com.github.bingoohuang.blackcat.instrument.utils.Asms.sig;
import static org.objectweb.asm.Opcodes.*;

public class TreeAsms {
    public static VarInsnNode getLoadInst(Type type, int position) {
        int opCode = getLoadOpCode(type);
        return new VarInsnNode(opCode, position);
    }

    private static int getLoadOpCode(Type type) {
        char c = type.getDescriptor().charAt(0);
        if ("BCIZS".indexOf(c) >= 0) return ILOAD;
        if (c == 'D') return DLOAD;
        if (c == 'F') return FLOAD;
        if (c == 'J') return LLOAD;
        if ("L[".indexOf(c) >= 0) return ALOAD;

        throw new ClassFormatError("Invalid method signature: " + type.getDescriptor());
    }

    private static int getStoreOpCode(Type type) {
        char c = type.getDescriptor().charAt(0);
        if ("BCIZS".indexOf(c) >= 0) return ISTORE;
        if ('D' == c) return DSTORE;
        if ('F' == c) return FSTORE;
        if ('J' == c) return LSTORE;
        if ("L[".indexOf(c) >= 0) return ASTORE;

        throw new ClassFormatError("Invalid method signature: " + type.getDescriptor());
    }

    public static InsnList getClassRefInst(Type type, int majorVersion) {
        char charType = type.getDescriptor().charAt(0);
        if (charType == '[' || charType == 'L')
            return getClassConstantRef(type, majorVersion);

        String wrapper = getWrapper(type, charType);

        InsnList list = new InsnList();
        list.add(new FieldInsnNode(GETSTATIC, wrapper, "TYPE", ci(Class.class)));
        return list;

    }

    static String[] wrapperDict = {
            "Bjava/lang/Byte",
            "Cjava/lang/Character",
            "Djava/lang/Double",
            "Fjava/lang/Float",
            "Ijava/lang/Integer",
            "Jjava/lang/Long",
            "Zjava/lang/Boolean",
            "Sjava/lang/Short"
    };

    private static String getWrapper(Type type, char charType) {
        for (String wrap : wrapperDict) {
            if (wrap.charAt(0) == charType) return wrap.substring(1);
        }
        throw new ClassFormatError("Invalid method signature: "
                + type.getDescriptor());
    }

    public static MethodInsnNode getWrapperCtorInst(Type type) {
        char charType = type.getDescriptor().charAt(0);
        String wrapper = null;
        for (String wrap : wrapperDict) {
            if (wrap.charAt(0) == charType) wrapper = wrap.substring(1);
        }

        if (wrapper == null) {
            if ("L[".indexOf(charType) >= 0) return null;
            throw new ClassFormatError("Invalid method signature: "
                    + type.getDescriptor());
        }

        return new MethodInsnNode(INVOKESTATIC, wrapper, "valueOf",
                "(" + charType + ")L" + wrapper + ";", false);
    }

    public static VarInsnNode getStoreInst(Type type, int position) {
        int opCode = getStoreOpCode(type);

        return new VarInsnNode(opCode, position);
    }

    public static AbstractInsnNode getPushInst(int value) {
        if (value >= -1 && value <= 5)
            return new InsnNode(ICONST_M1 + value + 1);
        if ((value >= -128) && (value <= 127))
            return new IntInsnNode(BIPUSH, value);
        if ((value >= -32768) && (value <= 32767))
            return new IntInsnNode(SIPUSH, value);

        return new LdcInsnNode(value);
    }

    public static InsnList getClassConstantRef(Type type, int majorVersion) {
        InsnList il = new InsnList();
        if (majorVersion >= V1_5) {
            il.add(new LdcInsnNode(type));
            return il;
        }

        String internalName = type.getInternalName();
        String fullyQualifiedName = internalName.replaceAll("/", ".");
        il.add(new LdcInsnNode(fullyQualifiedName));
        il.add(new MethodInsnNode(INVOKESTATIC,
                "java/lang/Class", "forName",
                sig(Class.class, String.class), false));
        return il;
    }

}
