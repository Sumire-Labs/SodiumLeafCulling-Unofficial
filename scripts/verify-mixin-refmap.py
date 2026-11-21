"""Verify declared Mixin refmaps and Forge's mapped block-face injection target."""

import argparse
import json
from zipfile import ZipFile


def verify(path):
    with ZipFile(path) as jar:
        configs = [name for name in jar.namelist()
                   if name.startswith("mixins.sodiumleafculling") and name.endswith(".json")
                   and not name.endswith(".refmap.json")]
        if not configs:
            raise ValueError("Runtime JAR contains no LeafCulling Mixin configuration")
        for config_name in configs:
            config = json.loads(jar.read(config_name))
            forge = config_name == "mixins.sodiumleafculling.forge.json"
            refmap_name = config.get("refmap")
            if not refmap_name and not forge:
                continue  # Fabric remaps inline; NeoForge uses named runtime targets.
            if not isinstance(refmap_name, str) or not refmap_name:
                raise ValueError(f"{config_name} does not declare a refmap")
            try:
                refmap = json.loads(jar.read(refmap_name))
            except KeyError as exc:
                raise ValueError(f"Runtime JAR is missing {refmap_name}") from exc
            mappings = refmap.get("mappings", {})
            if not mappings:
                raise ValueError(f"Refmap {refmap_name} contains no mappings")
            if forge:
                mixin = config["package"].replace(".", "/") + "/BlockOcclusionCacheMixin"
                entries = mappings.get(mixin, {})
                if not any(";skipRendering(" in source and target != source
                           for source, target in entries.items()):
                    raise ValueError("Refmap has no mapped BlockState.skipRendering injection target")
    print(f"Verified Mixin resources: {path}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar")
    args = parser.parse_args()
    verify(args.jar)
