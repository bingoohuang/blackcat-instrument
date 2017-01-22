package com.github.bingoohuang.blackcat.instrument.utils;

import com.github.bingoohuang.blackcat.instrument.annotations.BlackcatMonitor;
import com.google.common.io.Files;
import lombok.val;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;

public class Debugs {
    public static void writeClassFile(
            ClassNode cn, String className, byte[] bytes) {
        val annNode = Asms.getAnn(cn, BlackcatMonitor.class);
        if (annNode == null) return;
        if (!isDebug(annNode)) return;

        writeClassFile(className, bytes);
    }

    private static boolean isDebug(AnnotationNode annNode) {
        for (int i = 0, ii = annNode.values.size(); i < ii; i += 2) {
            String name = (String) annNode.values.get(i);
            Object value = annNode.values.get(i + 1);
            if ("debug".equals(name) && value.equals(true)) return true;
        }
        return false;
    }

    private static void writeClassFile(String className, byte[] bytes) {
        try {
            String classFilename = Asms.c(className) + ".class";
            Files.write(bytes, new File(classFilename));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
