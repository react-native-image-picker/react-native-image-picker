package com.imagepicker.permissions;

import androidx.annotation.NonNull;
import com.facebook.react.modules.core.PermissionListener;

/**
 * Created by rusfearuth on 25.02.17.
 */
public interface OnImagePickerPermissionsCallback
{
    void setPermissionListener(@NonNull PermissionListener listener);
}
