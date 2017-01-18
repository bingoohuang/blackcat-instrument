package com.github.bingoohuang.blackcat.javaagent.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class Helper {
    public static Object getSource(
            Class declaringClass,
            String name,
            Class... paramTypes) {
        if (name.equals("<init>")) {
            try {
                return declaringClass.getConstructor(paramTypes);
            } catch (Exception e) {
                // Anonymous classes
                return "init()";
            }
        }

        if (name.equals("<clinit>")) return "clinit()";

        try {
            return declaringClass.getDeclaredMethod(name, paramTypes);
        } catch (Exception e) {
            return name;
        }
    }

    public static int getArgPosition(int offset, Type[] arguments, int argNo) {
        int ret = argNo + offset;
        for (int i = 0; i < arguments.length && i < argNo; i++) {
            char charType = arguments[i].getDescriptor().charAt(0);
            if (charType == 'J' || charType == 'D') ++ret;
        }

        return ret;
    }

    public static boolean isAbstract(MethodNode m) {
        return (m.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public static boolean isStatic(MethodNode m) {
        return (m.access & Opcodes.ACC_STATIC) != 0;
    }

    public static boolean isPublic(MethodNode m) {
        return (m.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public static void viewByteCode(byte[] bytecode) {
        ClassReader cr = new ClassReader(bytecode);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        final List<MethodNode> mns = cn.methods;
        Printer printer = new Textifier();
        TraceMethodVisitor mp = new TraceMethodVisitor(printer);
        for (MethodNode mn : mns) {
            InsnList inList = mn.instructions;
            System.out.println(mn.name);
            for (int i = 0; i < inList.size(); i++) {
                inList.get(i).accept(mp);
                StringWriter sw = new StringWriter();
                printer.print(new PrintWriter(sw));
                printer.getText().clear();
                System.out.print(sw.toString());
            }
        }
    }
}
