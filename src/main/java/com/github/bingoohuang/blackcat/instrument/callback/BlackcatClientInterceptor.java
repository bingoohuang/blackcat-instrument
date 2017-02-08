package com.github.bingoohuang.blackcat.instrument.callback;

import com.github.bingoohuang.blackcat.instrument.annotations.BlackcatMonitor;
import com.github.bingoohuang.blackcat.instrument.discruptor.BlackcatClient;
import com.github.bingoohuang.blackcat.instrument.utils.Asms;
import com.github.bingoohuang.blackcat.sdk.utils.Blackcats;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static com.github.bingoohuang.blackcat.instrument.utils.Asms.*;
import static com.github.bingoohuang.blackcat.sdk.utils.Blackcats.readDiamond;
import static org.apache.commons.io.FilenameUtils.wildcardMatch;
import static org.n3r.diamond.client.impl.DiamondUtils.splitLinesWoComments;

@Slf4j
public class BlackcatClientInterceptor extends BlackcatJavaAgentInterceptorAdapter {
    @Override
    public boolean interceptClass(ClassNode classNode, String className) {
        return isAnnPresent(classNode, BlackcatMonitor.class)
                || isAnyMethodAnnPresent(classNode.methods, BlackcatMonitor.class)
                || isClassConfigured(className, classNode.methods);
    }

    @Override
    public boolean interceptMethod(ClassNode classNode, MethodNode methodNode) {
        return !isCtor(methodNode) && isAnnPresent(classNode, BlackcatMonitor.class)
                || isAnnPresent(methodNode, BlackcatMonitor.class)
                || isMethodConfigured(methodNode);
    }

    private boolean isCtor(MethodNode methodNode) {
        return "<init>".equals(methodNode.name);
    }

    @Override
    protected void onThrowableUncaught(BlackcatMethodRt rt) {
        BlackcatClient.send(rt);
    }

    @Override
    protected void onFinish(BlackcatMethodRt rt) {
        BlackcatClient.send(rt);
    }

    private boolean isClassConfigured(
            String className, // com/github/bingoohuang/springbootbank
            List<MethodNode> methodNodes) {
        if (!Blackcats.hasDiamond) return false;

        String config = readDiamond("blackcat^interceptClasses");
        if (StringUtils.isEmpty(config)) return false;

        val dottedClassName = className.replace('/', '.');
        val interceptClasses = splitLinesWoComments(config, "#");
        for (String interceptClass : interceptClasses) {
            if (wildcardMatch(dottedClassName, interceptClass)) return true;
        }

        return false;
    }

    private boolean isMethodConfigured(MethodNode methodNode) {
        if (!Blackcats.hasDiamond) return false;

        String config = readDiamond("blackcat^interceptMethods");
        if (StringUtils.isEmpty(config)) return false;

        val interceptMethods = splitLinesWoComments(config, "#");
        for (String interceptMethod : interceptMethods) {
            if (checkInterceptMethod(methodNode, interceptMethod)) return true;
        }

        return false;
    }

    private boolean checkInterceptMethod(MethodNode methodNode, String interceptMethod) {
        if (interceptMethod.startsWith("@")) {
            String wildAnnClassId = interceptMethod.substring(1);
            List visibleAnns = methodNode.visibleAnnotations;
            return isWildAnnPresent(wildAnnClassId, visibleAnns);
        }

        val methodTypeString = Asms.describeMethod(methodNode, false);
        log.debug("interceptMethod:{}, methodTypeString:{}", interceptMethod, methodTypeString);

        return wildcardMatch(methodTypeString, interceptMethod);
    }

}
