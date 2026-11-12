package dev.karma.calamitouscore.compat.stellara;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Test-only standalone Mixin host: no Minecraft launch or GL context. Never bundled in the mod JAR. */
public final class StellaraTestMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    public static Map<String, byte[]> fixtures = Map.of();
    public String getName() { return "CalamitousCore-Stellara-fixture"; }
    public boolean isValid() { return true; }
    public IClassProvider getClassProvider() { return this; }
    public IClassBytecodeProvider getBytecodeProvider() { return this; }
    public ITransformerProvider getTransformerProvider() { return null; }
    public IClassTracker getClassTracker() { return null; }
    public IMixinAuditTrail getAuditTrail() { return null; }
    public Collection<String> getPlatformAgents() { return List.of(); }
    public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("stellara-fixture"); }
    public InputStream getResourceAsStream(String name) { return getClass().getClassLoader().getResourceAsStream(name); }
    public URL[] getClassPath() { return new URL[0]; }
    public Class<?> findClass(String name) throws ClassNotFoundException { return findClass(name, false); }
    public Class<?> findClass(String name, boolean init) throws ClassNotFoundException { return Class.forName(name, init, getClass().getClassLoader()); }
    public Class<?> findAgentClass(String name, boolean init) throws ClassNotFoundException { return findClass(name, init); }
    public ClassNode getClassNode(String name) throws IOException, ClassNotFoundException { return getClassNode(name, true); }
    public ClassNode getClassNode(String name, boolean runTransformers) throws IOException, ClassNotFoundException {
        byte[] data = fixtures.get(name.replace('/', '.'));
        if (data == null) try (InputStream in = getResourceAsStream(name.replace('.', '/') + ".class")) {
            if (in == null) throw new ClassNotFoundException(name);
            data = in.readAllBytes();
        }
        ClassNode node = new ClassNode(); new ClassReader(data).accept(node, 0); return node;
    }
    public IMixinTransformer transformer() { return getInternal(IMixinTransformerFactory.class).createTransformer(); }
}
