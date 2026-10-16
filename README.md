# maninScript Compiler

A university compiler project for **maninScript**, a small programming language with colloquial Spanish syntax. The compiler implements the complete path from source code to a simple MIPS backend.

```text
Source
  -> Lexer
  -> Recursive-descent parser
  -> AST
  -> Semantic analysis
  -> Three-address-code IR
  -> Local optimization
  -> MIPS generation
```

## Why this project matters

This project is a strong example of the computer science foundations behind my engineering background. Rather than building around an existing framework, the work required reasoning about language design, parsing, type systems, intermediate representations, control flow and machine-level code generation.

## My contribution

This was a **team university project**. My contributions included:

- semantic analysis and type checking;
- parser error recovery;
- intermediate representation generation;
- parts of the MIPS backend;
- testing and debugging;
- technical documentation.

The project is presented as shared academic work and does not claim individual authorship of the complete compiler.

## Architecture

### Frontend
- Hand-written lexer.
- Recursive-descent parser.
- Abstract Syntax Tree for the language constructs.
- Syntax-error recovery based on FIRST/FOLLOW reasoning.

### Semantic analysis
- Nested symbol tables and scopes.
- Centralized type representation through `ValueType`.
- Compatibility checks for assignments, expressions, function calls and returns.
- Variable shadowing support.

### Intermediate representation
- Three-address-code style instructions.
- Internal names for scoped variables.
- Explicit labels and jumps.
- Short-circuit evaluation for boolean expressions.

### Backend
- MIPS code generation.
- Simple stack-based calling convention.
- Local dead-code elimination after unconditional control transfers.

## Run

Requires **JDK 21**.

```sh
javac -d out tests/SmokeTestRunner.java
java -cp out SmokeTestRunner
java -cp out Main tests/cases/valid/minimal_ms_extension.ms --ir-only
```

The compiler accepts a source file followed by an optional flag:

```text
(no flag)    frontend + semantics + IR + MIPS + Swing AST viewer
--ir-only    frontend + semantics + IR
--no-ast     generate MIPS without opening the AST viewer
```

## A small maninScript example

```text
laFaena pavos suma(pavos a, pavos b) {
    suelta a + b;
}

unTal pavos resultat = suma(2, 3);
```

## Testing

The repository includes valid and invalid programs under `tests/cases/`. `SmokeTestRunner.java` compiles the project and exercises valid programs, expected syntactic and semantic failures, and several MIPS-generation cases.

## Known limitations

- `di!` is supported through the intermediate representation but not by the MIPS backend.
- Arrays and structs are not implemented.
- Unary `!` and `!=` are not supported.
- The backend does not implement advanced register allocation.
- The MIPS calling convention is deliberately simple and project-specific.

## Code map

```text
src/Main.java                       entry point
src/lexer/                          lexical analysis
src/parser/                         recursive-descent parser
src/ast/                            abstract syntax tree
src/semantic/SemanticAnalyzer.java semantic analysis
src/types/ValueType.java            type system
src/ir/                             intermediate representation
src/mips/MipsGenerator.java         MIPS backend
tests/SmokeTestRunner.java          regression runner
```

## Context

Developed as part of the Programming Languages course in the Computer Engineering degree at La Salle - Ramon Llull University.

This repository is a curated portfolio version of the original team project. The original academic development repository remains private.
