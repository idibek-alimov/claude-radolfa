1. CLAUDE.md Files — Your Most Powerful Tool

  These are the instructions I read automatically in every conversation. You already have a good setup with 3 levels:

  CLAUDE.md              ← project-wide rules (architecture, commands)
  backend/CLAUDE.md      ← backend-specific rules
  frontend/CLAUDE.md     ← frontend-specific (price model, FSD rules, query keys)

  How to make them more effective:

  - Add "do not" rules from past mistakes. Example: if I once broke FSD by importing features/cart from features/checkout, add: Cross-slice imports are forbidden. I've violated this before — always extract to entities/.
  - Add canonical examples by file path: See CreateProductServiceTest.java for the fake adapter testing pattern.
  - Keep them short. I read them on every request. Dense walls of text dilute signal.

  Pro tip: If you notice me making the same mistake twice, add a one-liner to the relevant CLAUDE.md. That's permanent correction.

  ---
  2. Skills (Slash Commands) — Reusable Expert Workflows

  You have project-specific skills configured. Run them with /skill-name. The most relevant for Radolfa:

  ┌────────────────────┬───────────────────────────────────────────────────────────────────────────────────────┐
  │       Skill        │                                      When to use                                      │
  ├────────────────────┼───────────────────────────────────────────────────────────────────────────────────────┤
  │ /hexagonal-feature │ Scaffold a full vertical slice (domain → port → service → adapter → controller → DTO) │
  ├────────────────────┼───────────────────────────────────────────────────────────────────────────────────────┤
  │ /fsd-gen           │ Scaffold a new FSD slice with TanStack Query hooks + Shadcn                           │
  ├────────────────────┼───────────────────────────────────────────────────────────────────────────────────────┤
  │ /bridge            │ Sync Java DTOs/enums/read models → TypeScript interfaces in the right FSD layer       │
  ├────────────────────┼───────────────────────────────────────────────────────────────────────────────────────┤
  │ /audit-data        │ Validate that your changes respect Hexagonal + FSD + security rules                   │
  ├────────────────────┼───────────────────────────────────────────────────────────────────────────────────────┤
  │ /review            │ Senior code review: security (SQLi), performance, Hexagonal integrity                 │
  ├────────────────────┼───────────────────────────────────────────────────────────────────────────────────────┤
  │ /tdd-fix           │ Auto-diagnose and fix failing tests                                                   │
  └────────────────────┴───────────────────────────────────────────────────────────────────────────────────────┘

  Example workflow for a new feature:
  1. /hexagonal-feature  → scaffolds Java backend
  2. /bridge             → generates TypeScript types from the new Java DTOs
  3. /fsd-gen            → scaffolds the frontend feature slice
  4. /audit-data         → validates everything is wired correctly
  5. /review             → catches security/perf issues

  ---
  3. Hooks — Automated Actions on Events

  Hooks run shell commands automatically on certain events. Configure them in ~/.claude/settings.json. Examples useful for Radolfa:

  Auto-run tests after editing Java files:
  {
    "hooks": {
      "PostToolUse": [
        {
          "matcher": "Edit",
          "hooks": [
            {
              "type": "command",
              "command": "if echo '$TOOL_INPUT' | grep -q 'backend/src/main/java'; then cd /home/idibek/Desktop/ERP/claude-radolfa && ./mvnw test -pl backend -q 2>&1 | tail -5; fi"
            }
          ]
        }
      ]
    }
  }

  Auto-lint after editing TypeScript files:
  {
    "hooks": {
      "PostToolUse": [
        {
          "matcher": "Edit",
          "hooks": [
            {
              "type": "command",
              "command": "if echo '$TOOL_INPUT' | grep -q 'frontend/src'; then cd /home/idibek/Desktop/ERP/claude-radolfa && npm run lint --prefix frontend 2>&1 | tail -10; fi"
            }
          ]
        }
      ]
    }
  }

  Use /update-config skill to configure hooks without editing JSON manually.

  ---
  4. Agents — Parallel and Specialized Work

  I can spawn subagents to do work in parallel or protect the main conversation from large search results.

  When I use them automatically:
  - Deep codebase exploration (finding all usages of a pattern across 50+ files)
  - Running a code review while I continue writing
  - Research that might produce huge output

  How you can direct me to use them:

  "Explore the entire order checkout flow end-to-end (backend + frontend)
  and give me a map of all files involved."
  → I'll spawn an Explore agent so the main context isn't polluted.

  "Implement the new wishlist feature. Run backend and frontend work in parallel."
  → I'll spawn two agents simultaneously — one for Java, one for Next.js.

  Worktree isolation — for risky refactors, I can work in an isolated git branch:
  "Refactor the ProductHierarchyAdapter in an isolated worktree so nothing breaks main."

  ---
  5. Memory System — What Persists Across Conversations

  I maintain memory in /home/idibek/.claude/projects/.../memory/. Currently saved:

  - Your VPS SSH details
  - POSAwesome fix history
  - ERPNext setup

  What you should ask me to remember:
  - Decisions you've made about architecture ("remember: we decided not to use Zustand")
  - Team preferences ("remember: always show loading skeletons, never spinners")
  - Bugs that recur ("remember: Flyway migration V7 failed due to missing index — check column names")
  - Workflow preferences ("remember: always run tests before suggesting a commit")

  What I already auto-save: corrections you give me, your role/expertise signals, project decisions.

  ---
  6. MCP Servers — Live Data Access

  You have two MCP servers active:

  context7 — Live library documentation. Use when:
  "How do I configure TanStack Query v5 prefetching with Next.js App Router?"
  → I'll fetch current docs, not rely on training data that may be stale.

  postgres — Direct database queries. Use for:
  "What's the current schema for the orders table?"
  "Run a quick sanity check — how many products have no SKUs?"

  ---
  7. The FRONTEND_INTEGRATION_PLAN.md Pattern

  You already have this — it's smart. It tracks which phases are done vs. pending. Make it more useful:

  - Before asking me to build a feature, say: "Check FRONTEND_INTEGRATION_PLAN.md first"
  - After I finish a phase, I should mark it done — remind me if I forget
  - Add estimated complexity per phase so I can sequence work better

  ---
  8. High-Leverage Prompting Patterns

  Be specific about scope:
  "Add the wishlist feature. Backend only for now — Java domain model + port + service. No controller yet."

  Reference canonical examples:
  "Write the test for UpdateProductNameService the same way CreateProductServiceTest is structured."

  Constrain the output:
  "Only modify ListingVariant.java and its mapper. Don't touch any other files."

  Chain skills:
  /hexagonal-feature CreateWishlistUseCase
  Then after it runs:
  /bridge
  Then:
  /fsd-gen wishlist feature

  Ask for a plan before code on complex tasks:
  "Plan (don't code yet) how to add a product comparison feature. Show me the files you'd touch."
  → This lets you catch architectural mistakes before I write 500 lines.

  ---
  9. Quick Reference: What to Use When

  ┌──────────────────────────────────┬─────────────────────────────────────────┐
  │               Goal               │                  Tool                   │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ New backend vertical slice       │ /hexagonal-feature                      │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ New frontend feature             │ /fsd-gen                                │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Sync Java types to TypeScript    │ /bridge                                 │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Validate architecture compliance │ /audit-data                             │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Fix failing tests                │ /tdd-fix                                │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Deep codebase exploration        │ Ask me to use an Explore agent          │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Parallel backend + frontend work │ Ask me to run agents in parallel        │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Teach me a permanent rule        │ Ask me to update CLAUDE.md or remember: │
  ├──────────────────────────────────┼─────────────────────────────────────────┤
  │ Configure automated behavior     │ /update-config (hooks)                  │
  └──────────────────────────────────┴─────────────────────────────────────────┘

  ---
  The single highest-ROI action: Keep your three CLAUDE.md files sharp. Every rule there saves you from having to repeat it. When I make an error, add a one-liner preventing it from ever happening again.
