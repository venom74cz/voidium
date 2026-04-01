# VOID Workflow

Use the full VOID workflow for this task.

This prompt is the all-in-one mode. It includes the core engineering rules, UI and UX quality bar, cleanup discipline, documentation discipline, and production-style delivery standard.

Execution workflow:

1. Inspect the real codebase and the most relevant local docs before proposing or implementing changes.
2. Build enough context to understand the real architecture, constraints, and product intent.
3. Choose the most modern stable solution that fits this repository.
4. Implement the real feature, fix, refactor, UI, docs, or workflow change end-to-end.
5. Do not leave TODO, FIXME, placeholder text, fake data, stub logic, dead branches, commented-out future code, or half-finished scaffolding.
6. Clean up local mess introduced by the task instead of stacking more complexity on top.
7. Update docs or config when behavior, setup, architecture, workflow, or user-facing output changes.
8. Validate with the strongest reasonable local check.

Engineering bar:

- Prefer maintainable structure, explicit contracts, typed code where available, predictable data flow, and useful error handling.
- Improve known weak areas when they directly block quality.
- Do not choose low-effort shortcuts unless explicitly requested.

UI and UX bar:

- Keep the design intentional, polished, responsive, accessible, and visually coherent.
- Start meaningful UI work by defining a compact design system: hierarchy, typography, colors, spacing, surfaces, icon style, and motion.
- Avoid generic template-looking layouts when the product needs a premium feel.
- Do not use emoji as UI icons.
- Ensure interactive elements look interactive, have stable hover behavior, and have visible focus states.
- Check mobile layout, contrast, readability, and interaction quality before calling UI work done.

When you respond, include:

- what changed
- how it was validated
- any remaining risk or assumption
