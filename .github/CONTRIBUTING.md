# Contributing Guidelines

Thank you for considering contributing to Telegram Media Downloader! We welcome contributions from everyone.

## How to Contribute

### Reporting Bugs
- Use the bug report template
- Provide detailed reproduction steps
- Include environment information
- Check if the issue already exists

### Suggesting Features
- Use the feature request template
- Explain the use case clearly
- Consider backwards compatibility
- Be open to discussion

### Code Contributions

#### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Ensure all tests pass
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

#### Coding Standards
- Follow existing code style
- Write meaningful commit messages
- Add tests for new functionality
- Update documentation when needed
- Keep PRs focused and small

#### Testing
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=YourTestClass

# Skip tests during development
./mvnw package -DskipTests
```

#### Docker Testing
```bash
# Build and test Docker image locally
docker build -t tmd-test .
docker run -d --name tmd-test-container -p 3222:3222 tmd-test
```

## Development Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker (optional but recommended)

### Local Development
```bash
# Clone and setup
git clone https://github.com/your-username/telegram-media-downloader.git
cd telegram-media-downloader

# Configure environment
cp .env.example .env
# Edit .env with your Telegram credentials

# Create required directories
mkdir -p data downloads/videos downloads/thumbnails downloads/temp logs

# Build and run
./mvnw spring-boot:run
```

## Pull Request Process

1. Ensure your code follows the project's coding standards
2. Update the README.md with details of changes to the interface
3. Increase the version numbers in any examples files and the README.md to the new version that this Pull Request would represent
4. Your Pull Request will be reviewed by maintainers
5. Address any feedback provided during review

## Code of Conduct

### Our Pledge
In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making participation in our project and our community a harassment-free experience for everyone.

### Our Standards
Examples of behavior that contributes to creating a positive environment include:
- Using welcoming and inclusive language
- Being respectful of differing viewpoints and experiences
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

## Questions?

Feel free to ask questions in:
- GitHub Issues
- Discussion forums
- Community chat (if available)

Thank you for contributing!