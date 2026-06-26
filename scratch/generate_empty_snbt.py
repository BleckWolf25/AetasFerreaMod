import json
import os

size = [5, 5, 5]
data = []

for x in range(size[0]):
    for y in range(size[1]):
        for z in range(size[2]):
            data.append({
                "pos": [x, y, z],
                "state": "minecraft:air"
            })

snbt = {
    "DataVersion": 3465,
    "size": size,
    "data": data,
    "entities": [],
    "palette": ["minecraft:air"]
}

# SNBT is not strict JSON, but python's str of a dict without quotes on keys is somewhat close
# However, for a simple SNBT, Minecraft's NBT parser actually accepts JSON if formatted properly!
# Wait, no, it expects unquoted keys for SNBT. But we can just use python to format it.

def to_snbt(obj):
    if isinstance(obj, dict):
        return "{" + ",".join(f"{k}:{to_snbt(v)}" for k, v in obj.items()) + "}"
    elif isinstance(obj, list):
        return "[" + ",".join(to_snbt(v) for v in obj) + "]"
    elif isinstance(obj, str):
        return f'"{obj}"'
    elif isinstance(obj, int):
        return str(obj)
    return str(obj)

snbt_str = to_snbt(snbt)

os.makedirs("src/test/resources/data/minecraft/gameteststructures", exist_ok=True)
with open("src/test/resources/data/minecraft/gameteststructures/empty.snbt", "w") as f:
    f.write(snbt_str)
