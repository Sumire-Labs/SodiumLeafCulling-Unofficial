package toni.sodiumleafculling;

//? if fabric {
import java.util.List;
import com.bawnorton.mixinsquared.api.MixinCanceller;

public class LeafCullingMixinCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targetClassNames, String mixinClassName) {
        // MixinSquared supplies the fully-qualified class name. Match the
        // stable mixin class suffix so MoreCulling package moves remain safe.
        return mixinClassName.endsWith(".LeavesBlock_typesMixin");
    }
}
//?}
