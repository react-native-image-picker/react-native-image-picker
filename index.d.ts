declare module "react-native-image-picker" {

    interface ImagePickerResponse {
        customButton: string;
        didCancel: boolean;
        error: string;
        data: string;
        uri: string;
        origURL?: string;
        isVertical: boolean;
        width: number;
        height: number;
        fileSize: number;
        type?: string;
        fileName?: string;
        path?: string;
        latitude?: number;
        longitude?: number;
        timestamp?: string;
    }

    interface ImagePickerCustomButtonOptions {
        name?: string;
        title?: string;
    }

    interface ImagePickerOptions {
        title?: string;
        cancelButtonTitle?: string;
        takePhotoButtonTitle?: string;
        chooseFromLibraryButtonTitle?: string;
        customButtons?: Array<ImagePickerCustomButtonOptions>;
        cameraType?: 'front' | 'back';
        mediaType?: 'photo' | 'video' | 'mixed';
        maxWidth?: number;
        maxHeight?: number;
        quality?: number;
        videoQuality?: 'low' | 'medium' | 'high';
        durationLimit?: number;
        rotation?: number;
        allowsEditing?: boolean;
        noData?: boolean;
        storageOptions?: ImagePickerStorageOptions;
    }

    interface ImagePickerStorageOptions {
        skipBackup?: boolean;
        path?: string;
        cameraRoll?: boolean;
        waitUntilSaved?: boolean;
    }


    export default class ImagePicker {
        static showImagePicker(options: ImagePickerOptions, callback: (response: ImagePickerResponse) => void): void;
        static launchCamera(options: ImagePickerOptions, callback: (response: ImagePickerResponse) => void): void;
        static launchImageLibrary(options: ImagePickerOptions, callback: (response: ImagePickerResponse) => void): void;
    }

}
