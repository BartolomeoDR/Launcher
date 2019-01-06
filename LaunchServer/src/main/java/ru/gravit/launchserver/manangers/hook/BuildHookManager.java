package ru.gravit.launchserver.manangers.hook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import ru.gravit.launcher.AutogenConfig;
import ru.gravit.launcher.modules.TestClientModule;
import ru.gravit.launchserver.binary.BuildContext;
import ru.gravit.launchserver.binary.JAConfigurator;
import ru.gravit.launchserver.binary.tasks.MainBuildTask;

public class BuildHookManager {
    @FunctionalInterface
    public interface ZipBuildHook {
        void build(ZipOutputStream context);
    }

    @FunctionalInterface
    public interface BuildHook {
        void build(BuildContext context);
    }

    @FunctionalInterface
    public interface Transformer {
        byte[] transform(byte[] input, String classname, MainBuildTask data);
    }

    private boolean BUILDRUNTIME;
    private final Set<BuildHook> POST_HOOKS;
    private final Set<Runnable> POST_PROGUARDRUN_HOOKS;
    private final Set<Transformer> POST_PROGUARD_HOOKS;
    private final Set<BuildHook> PRE_HOOKS;
    private final Set<ZipBuildHook> POST_PROGUARD_BUILDHOOKS;
    private final Set<Transformer> CLASS_TRANSFORMER;
    private final Set<String> CLASS_BLACKLIST;
    private final Set<String> MODULE_CLASS;
    private final Map<String, byte[]> INCLUDE_CLASS;

    public BuildHookManager() {
        POST_HOOKS = new HashSet<>(4);
        POST_PROGUARDRUN_HOOKS = new HashSet<>(4);
        POST_PROGUARD_HOOKS = new HashSet<>(4);
        PRE_HOOKS = new HashSet<>(4);
        POST_PROGUARD_BUILDHOOKS = new HashSet<>(4);
        CLASS_BLACKLIST = new HashSet<>(4);
        MODULE_CLASS = new HashSet<>(4);
        INCLUDE_CLASS = new HashMap<>(4);
        CLASS_TRANSFORMER = new HashSet<>(4);
        BUILDRUNTIME = true;
        autoRegisterIgnoredClass(AutogenConfig.class.getName());
        registerIgnoredClass("META-INF/DEPENDENCIES");
        registerIgnoredClass("META-INF/LICENSE");
        registerIgnoredClass("META-INF/NOTICE");
        registerClientModuleClass(TestClientModule.class.getName());
    }

    public Set<ZipBuildHook> getProguardBuildHooks() {
        return POST_PROGUARD_BUILDHOOKS;
    }

    public Set<Runnable> getPostProguardRunHooks() {
        return POST_PROGUARDRUN_HOOKS;
    }

    public void addPostProguardRunHook(Runnable hook) {
        POST_PROGUARDRUN_HOOKS.add(hook);
    }

    public void addPostProguardRunHook(ZipBuildHook hook) {
        POST_PROGUARD_BUILDHOOKS.add(hook);
    }

    public void autoRegisterIgnoredClass(String clazz) {
        CLASS_BLACKLIST.add(clazz.replace('.', '/').concat(".class"));
    }

    public boolean buildRuntime() {
        return BUILDRUNTIME;
    }

    public byte[] classTransform(byte[] clazz, String classname, MainBuildTask reader) {
        byte[] result = clazz;
        for (Transformer transformer : CLASS_TRANSFORMER) result = transformer.transform(result, classname, reader);
        return result;
    }

    public byte[] proGuardClassTransform(byte[] clazz, String classname, MainBuildTask reader) {
        byte[] result = clazz;
        for (Transformer transformer : POST_PROGUARD_HOOKS) result = transformer.transform(result, classname, reader);
        return result;
    }

    public void registerIncludeClass(String classname, byte[] classdata) {
        INCLUDE_CLASS.put(classname, classdata);
    }

    public Map<String, byte[]> getIncludeClass() {
        return INCLUDE_CLASS;
    }

    public boolean isContainsBlacklist(String clazz) {
        for (String classB : CLASS_BLACKLIST) {
            if (clazz.startsWith(classB)) return true;
        }
        return false;
    }

    public void postHook(BuildContext context) {
        for (BuildHook hook : POST_HOOKS) hook.build(context);
    }

    public void preHook(BuildContext context) {
        for (BuildHook hook : PRE_HOOKS) hook.build(context);
    }

    public void registerAllClientModuleClass(JAConfigurator cfg) {
        for (String clazz : MODULE_CLASS) cfg.addModuleClass(clazz);
    }

    public void registerClassTransformer(Transformer transformer) {
        CLASS_TRANSFORMER.add(transformer);
    }

    public void registerClientModuleClass(String clazz) {
        MODULE_CLASS.add(clazz);
    }

    public void registerIgnoredClass(String clazz) {
        CLASS_BLACKLIST.add(clazz);
    }

    public void registerPostHook(BuildHook hook) {
        POST_HOOKS.add(hook);
    }

    public void registerProGuardHook(Transformer hook) {
        POST_PROGUARD_HOOKS.add(hook);
    }

    public boolean isNeedPostProguardHook() {
        return POST_PROGUARD_HOOKS.size() > 1 || !POST_PROGUARDRUN_HOOKS.isEmpty() || !POST_PROGUARD_BUILDHOOKS.isEmpty();
    }

    public void registerPreHook(BuildHook hook) {
        PRE_HOOKS.add(hook);
    }

    public void setBuildRuntime(boolean runtime) {
        BUILDRUNTIME = runtime;
    }
}