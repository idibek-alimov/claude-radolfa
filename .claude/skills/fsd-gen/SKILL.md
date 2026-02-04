---
name: fsd-gen
description: Scaffold a new FSD slice (Entity, Feature, or Widget) with correct folder structure.
---

# FSD Scaffolding Protocol
For the given name ($ARGUMENTS) and layer (entities/features/widgets):
1. Create the directory structure:
   - `src/$LAYER/$NAME/ui/`
   - `src/$LAYER/$NAME/model/`
   - `src/$LAYER/$NAME/api/`
2. Create an `index.ts` in the root of the slice to act as the Public API.
3. Create a basic TypeScript type/interface in `model/types.ts`.
4. Create a skeleton React component in `ui/`.

**Output:** A tree view of the created structure.