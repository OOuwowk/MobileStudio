# Mobile Studio - Android IDE

Mobile Studio is a comprehensive Integrated Development Environment (IDE) for Android that runs directly on Android devices. It allows developers to create, edit, and build Android applications directly from their mobile devices.

## Features

- **Advanced Code Editor** with syntax highlighting and auto-completion
- **Project Manager** for creating and organizing app projects
- **UI Designer** with drag-and-drop interface
- **Compiler** for building apps directly on the device
- **Debugger** for finding and fixing issues
- **Project Templates** to quickly start new projects

## Architecture

Mobile Studio is built with a modular architecture:

- **app**: Main application module
- **editor-module**: Code editor implementation
- **designer-module**: UI designer implementation
- **compiler-module**: Compiler and build system
- **debugger-module**: Debugging tools

## Development

### Prerequisites

- Android Studio 4.2+
- JDK 11
- Android SDK 30+

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build the project

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [Ace Editor](https://ace.c9.io/) for the code editing component
- [Android Jetpack](https://developer.android.com/jetpack) libraries
- [Dagger Hilt](https://dagger.dev/hilt/) for dependency injection