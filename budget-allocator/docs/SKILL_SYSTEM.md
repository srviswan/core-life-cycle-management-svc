# Simplified Skill System with Regex Support

## Overview

The budget allocator now supports a **simplified skill format** with regex pattern matching, making it easier to specify skill requirements without complex JSON structures.

## Simple String Format (Recommended)

### Basic Syntax

- **Comma (`,`)** = AND - all skills required
- **Pipe (`|`)** = OR - at least one skill required
- **Ampersand (`&`)** = AND (alternative to comma)

### Examples

#### 1. Simple AND (all required)
```
python,java,sql
```
Requires: python **AND** java **AND** sql

#### 2. Simple OR (at least one)
```
python|java|sql
```
Requires: python **OR** java **OR** sql (at least one)

#### 3. Mixed AND/OR
```
python,java|sql
```
Requires: (python **AND** java) **OR** sql

```
python&java|sql
```
Same as above (using `&` instead of `,`)

## Regex Support

### Automatic Regex Detection

Regex patterns are automatically detected if they contain regex special characters:
- `*`, `+`, `?` (quantifiers)
- `^`, `$` (anchors)
- `[`, `]` (character classes)
- `(`, `)` (groups)
- `|`, `\` (alternation, escape)

### Explicit Regex Prefix

You can also explicitly mark regex patterns with `regex:` prefix:
```
regex:python.*
regex:java[0-9]+
```

### Regex Examples

#### Match any Python version
```
python.*|java
```
Matches: `python`, `python3`, `python2.7`, etc. **OR** `java`

#### Match Java versions
```
regex:java[0-9]+|scala
```
Matches: `java8`, `java11`, `java17`, etc. **OR** `scala`

#### Match skills starting with "data"
```
data.*
```
Matches: `data`, `database`, `datawarehouse`, etc.

## JSON Format (Legacy, Still Supported)

For backward compatibility, the JSON format is still supported:

```json
{
  "mandatory_and": ["python"],
  "mandatory_or": ["java", "c++"],
  "technical_and": ["sql"],
  "technical_or": ["pandas"],
  "functional_and": ["pricing"],
  "functional_or": ["risk", "trading"]
}
```

You can also use simple strings in JSON fields:
```json
{
  "technical": "python,java|sql",
  "functional": "pricing|risk"
}
```

## How It Works

1. **Parsing**: The system parses skill strings and detects:
   - AND operators (`,` or `&`)
   - OR operators (`|`)
   - Regex patterns (auto-detected or `regex:` prefix)

2. **Matching**: When checking if a resource matches:
   - **AND skills**: All must match (using regex if pattern detected)
   - **OR skills**: At least one must match (using regex if pattern detected)

3. **Regex Matching**: 
   - Case-insensitive matching
   - Searches within comma-separated skill lists
   - Falls back to literal match if regex is invalid

## Examples in Excel

### Example 1: Simple Technical Skills
```
required_skills: python,java,sql
```
All three skills required.

### Example 2: Flexible Technical Skills
```
required_skills: python|java|scala
```
Any one of the three skills.

### Example 3: Regex Pattern Matching
```
required_skills: python.*|java[0-9]+
```
Matches Python (any version) OR Java (with version number).

### Example 4: Mixed Requirements
```
required_skills: python,sql|java
```
Requires (python AND sql) OR java.

## Best Practices

1. **Use simple format** for most cases: `python,java,sql`
2. **Use regex** for flexible matching: `python.*|java`
3. **Use JSON format** only if you need separate technical/functional/mandatory categories
4. **Test regex patterns** to ensure they match as expected
5. **Keep it simple** - avoid overly complex nested AND/OR combinations

## Migration from JSON Format

If you're currently using JSON format, you can migrate to simple format:

**Before (JSON):**
```json
{
  "technical_and": ["python", "java"],
  "technical_or": ["sql", "pandas"]
}
```

**After (Simple):**
```
python,java|sql,pandas
```

Note: The simple format treats `python,java|sql,pandas` as (python AND java) OR (sql AND pandas). For more complex logic, you may still need JSON format.
