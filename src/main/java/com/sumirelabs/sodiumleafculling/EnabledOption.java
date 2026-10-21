package com.sumirelabs.sodiumleafculling;

//? if !sodium_modern_config && !embeddium_modern {
/*import java.util.Collection;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;

//? if sodium_caffeine {
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionFlag;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
//?} else {
/^import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
^///?}

/^* Supplies dynamic availability to controls on renderers without dependent option support. ^/
public final class EnabledOption<T> implements Option<T> {
    private final Option<T> delegate;
    private final BooleanSupplier enabled;

    public EnabledOption(Option<T> delegate, BooleanSupplier enabled) {
        this.delegate = delegate;
        this.enabled = enabled;
    }


    //? if string_options {
    //public String getName() { return delegate.getName(); }
    //?} else {
    public Component getName() { return delegate.getName(); }
    //?}
    public Component getTooltip() { return delegate.getTooltip(); }
    public OptionImpact getImpact() { return delegate.getImpact(); }
    public Control<T> getControl() { return delegate.getControl(); }
    public T getValue() { return delegate.getValue(); }
    public void setValue(T value) { if (isAvailable()) delegate.setValue(value); }
    public void reset() { delegate.reset(); }
    public OptionStorage<?> getStorage() { return delegate.getStorage(); }
    public boolean isAvailable() { return delegate.isAvailable() && enabled.getAsBoolean(); }
    public boolean hasChanged() { return delegate.hasChanged(); }
    public void applyChanges() { delegate.applyChanges(); }
    public Collection<OptionFlag> getFlags() { return delegate.getFlags(); }
}
*///?}
