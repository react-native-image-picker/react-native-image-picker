package com.imagepicker;

import androidx.core.content.FileProvider;

// Two FileProvider class with same name will conflict during manifest merger, so we create a class which inherits from FileProvider with different name.
// This prevents conflict if library users already have used the default FileProvider class.
public class ImagePickerProvider extends FileProvider {
}
