package com.github.bingoohuang.blackcat.instrument.instrument;

import lombok.SneakyThrows;
import lombok.val;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class BlackcatJavaAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation
    ) throws InstantiationException {
        try {
            instrumentation.addTransformer(new AgentTransformer());
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    static class AgentTransformer implements ClassFileTransformer {
        public byte[] transform(final ClassLoader loader,
                                final String className,
                                final Class<?> classBeingRedefined,
                                final ProtectionDomain protectionDomain,
                                final byte[] classfileBuffer)
                throws IllegalClassFormatException {

            if (!isAncestor(BlackcatJavaAgent.class.getClassLoader(), loader))
                return classfileBuffer;

            return AccessController.doPrivileged(new PrivilegedAction<byte[]>() {
                public byte[] run() {
                    return instrument(classfileBuffer);
                }
            });
        }
    }

    @SneakyThrows
    private static byte[] instrument(byte[] classfileBuffer) {
        try {
            val blackcatInst = new BlackcatInstrument(classfileBuffer);
            return blackcatInst.modifyClass().y;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static boolean isAncestor(ClassLoader ancestor, ClassLoader cl) {
        if (ancestor == null || cl == null) return false;
        if (ancestor.equals(cl)) return true;

        return isAncestor(ancestor, cl.getParent());
    }
}
