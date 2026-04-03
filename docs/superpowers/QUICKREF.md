# Superpowers Quick Reference

Quick guide for using superpowers workflows in ok-inv-app.

## 🚀 Starting a New Feature

```
1. Say: "Let's brainstorm [feature name]"
2. Answer clarifying questions about UI, data flow, navigation
3. Review proposed approaches
4. Approve design
5. AI creates implementation plan
6. Execute with TDD
```

## 🧪 Test-Driven Development Cycle

```bash
# 1. RED - Write failing test
./gradlew test --tests "MyTest"
# Should FAIL

# 2. GREEN - Minimal implementation
# Write just enough code to pass

# 3. VERIFY
./gradlew test --tests "MyTest"
# Should PASS

# 4. REFACTOR (optional)
# Clean up while keeping tests green

# 5. COMMIT
git add . && git commit -m "feat: add feature with tests"
```

## 🐛 Debugging Process

```
1. Say: "Debug [issue description]"
2. AI checks logcat and traces root cause
3. AI writes failing test proving bug
4. AI implements fix
5. AI verifies fix on tests
```

## 👀 Code Review

```
After completing a feature:
"Review this code against the plan"

AI will check:
- Plan alignment
- Android best practices
- Memory leaks
- Threading issues
- Test coverage
```

## 📝 File Locations

```
Design Docs:      docs/superpowers/specs/YYYY-MM-DD-feature-name.md
Implementation:   docs/superpowers/plans/YYYY-MM-DD-feature-name.md
Skills:           .github/copilot/skills/
Agents:           .github/copilot/agents/
```

## 🔧 Common Commands

```bash
# Unit tests
./gradlew test

# Specific test
./gradlew test --tests "ScannerViewModelTest"

# Instrumented tests
./gradlew connectedAndroidTest

# Quick deploy
./gradlew quickDeploy

# Check logcat
adb logcat -s "ok.inv:*"

# Build release
./gradlew assembleRelease
```

## ✅ Before Committing Checklist

- [ ] Design document exists (if new feature)
- [ ] Tests written BEFORE implementation
- [ ] All tests pass
- [ ] No main thread violations
- [ ] No Context/View leaks in ViewModels
- [ ] Code reviewed (manually or by agent)

## 🎯 Key Principles

1. **Design First** - No coding before approved design
2. **Test First** - No implementation before failing test
3. **Root Cause First** - No fixes before investigation
4. **Review First** - No merge before code review

## 📚 Full Documentation

See `.github/copilot/README.md` for complete guide.

---

**Remember:** These workflows save time by catching issues early! 🎉
