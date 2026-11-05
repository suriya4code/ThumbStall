# Contributing to ThumbStall

Thank you for your interest in contributing!

## Code of Conduct

Be respectful, be kind, assume good intent.

## Getting Started

1. Fork the repository
2. Clone your fork
3. Create a branch from `main`
4. Make your changes
5. Run tests: `./gradlew :core:test`
6. Build: `./gradlew assembleFossDebug`
7. Open a pull request

## Project Structure

- **`app/`** — Android app (Compose UI, services, Room)
- **`core/`** — Pure Kotlin rules engine (TDD, no Android deps)

The `core` module is heavily tested. The `app` module relies on manual smoke testing (see `TESTING.md`).

## Guidelines

### Code Style
- Kotlin with Compose
- Follow existing patterns in the codebase
- Keep commits focused and atomic
- Use conventional commit messages: `feat:`, `fix:`, `docs:`, `test:`, `chore:`

### Testing
- New logic in `core/` should have JUnit tests
- Run `./gradlew :core:test` before submitting
- For UI changes, manually test on a device using `TESTING.md`

### Pull Requests
- Keep PRs small and focused
- Describe what changed and why
- Reference any related issues

## Architecture Decisions

Key decisions are documented in:
- `docs/superpowers/specs/` — Design specs
- `docs/superpowers/plans/` — Implementation plans

## Questions?

Open an issue for questions or discussion.

## License

By contributing, you agree that your contributions will be licensed under GPL-3.0.
