package dev.karma.calamitouscore.compat.stellara;
import java.util.*;
import org.spongepowered.asm.service.*;
public final class StellaraTestProperties implements IGlobalPropertyService {
    private final Map<IPropertyKey,Object> map = new HashMap<>();
    private record Key(String name) implements IPropertyKey {}
    public IPropertyKey resolveKey(String name) { return new Key(name); }
    @SuppressWarnings("unchecked") public <T> T getProperty(IPropertyKey key) { return (T) map.get(key); }
    public void setProperty(IPropertyKey key, Object value) { map.put(key, value); }
    public <T> T getProperty(IPropertyKey key, T def) { T value = getProperty(key); return value == null ? def : value; }
    public String getPropertyString(IPropertyKey key, String def) { Object value = map.get(key); return value == null ? def : value.toString(); }
}
