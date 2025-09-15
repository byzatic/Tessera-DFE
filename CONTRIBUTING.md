# Contributing to Tessera-DFE

We welcome contributions from the community!  
Please follow the guidelines below to help us maintain code quality and consistency.

## Development workflow
1. Create Issue.
2. Fork the repository.
3. Create a new branch: `git checkout -b feature-issue-<Issue number>`.
4. Make your changes and commit with clear messages. 
5. Push to your fork and open a Pull Request.

## Code Style
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- Run `mvn spotless:apply` before pushing.

## Testing
- Run all unit tests before submitting a PR:
```bash
mvn clean test
```

## Commit messages
- Use present tense: "Add feature" not "Added feature".
- Best practice `<action>: Message` for example `bugfix: Add feature`
- Limit the first line to 72 characters.
- Reference issues with `#<number>` when applicable.

## Reporting Issues
- Provide steps to reproduce the bug.
- Include logs, stacktraces, or screenshots when relevant.

## License
By contributing, you agree that your contributions will be licensed under the Apache-2.0 License.