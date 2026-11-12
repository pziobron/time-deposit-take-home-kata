# AI-assisted development

## Tools and setup

Development uses the Codex agent integrated into IntelliJ IDEA, with access to
the local repository and terminal. The agent identifies itself as based on GPT-6;
the exact model build and IDE-managed system configuration are not pinned in this
repository. No additional agent framework or project-specific agent configuration
has been introduced at this stage.

The Java module targets Java 17 and uses Maven, JUnit 5, and AssertJ. The initial
test suite was run locally with OpenJDK 25.0.1 and Maven 3.9.9.

## Workflow and task instructions

The following instructions summarize the task-specific prompts used so far and
can be reused with a coding agent against this repository:

1. Read the README, existing calculator, shared model, and tests before proposing
   changes. Identify behavioral boundaries and refactoring constraints.
2. Add characterization tests for `TimeDepositCalculator` without changing
   production code. Preserve existing arithmetic and observable results.
3. Use JUnit parameterized tests for repeated input cases. Test outcomes rather
   than implementation details; explain the purpose of each case.
4. Keep the shared `TimeDeposit` contract and the `updateBalance` method signature
   unchanged, as required by the assignment.
5. Run the Java test suite after changes and review the diff. Review generated
   code and expected values with the candidate before proceeding to refactoring.

These are a summary of the substantive task instructions used. Explanatory
questions and the full conversation are omitted. No separate custom system prompt
or agent configuration file has been created for this project. If either is added
later, it should be included in the repository and referenced here. Equivalent
instructions reproduce the workflow; generated code may vary between model
versions and runs.

To run the checks from the repository root, with Java 17 or newer and Maven installed:

```sh
cd java
mvn test
```

## Work assisted so far

- Requirements analysis and planning of a behavior-preserving refactor.
- Replacement of the placeholder assertion with calculator characterization tests:
  plan rates, eligibility boundaries, rounding, zero balances, unknown plans,
  collection handling, repeated updates, and preservation of deposit metadata.
- Conversion of dynamic tests to parameterized tests and addition of the matching
  JUnit dependency.
- Explanation and review of binary floating-point rounding, including why changing
  the monetary arithmetic could violate the requirement to preserve behavior.
- Drafting of this documentation.
- Documentation of calculator assumptions in code comments: annual rates divided
  by 12 and one update per invocation, without scheduling or advancing days.
- Initial calculator refactoring: for-each iteration, extraction of interest
  calculation, and guard clauses, preserving arithmetic and rounding.

AI assistance supported test design and implementation before refactoring. The
candidate reviewed the cases interactively, questioned expected values and
redundancy, and discussed the behavior each test protects.

The calculator has undergone an initial structural refactor without intentional
behavior changes. The latest test run passed all 37 tests. No coverage measurement has been performed yet. This document should be
updated as further development is completed.
