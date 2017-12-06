package io.goobox.sync.storj.helpers;

import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.Storj;

public class StorjUtil {

    public static void deleteFile(File file) {
        Storj.getInstance().deleteFile(null, file, new TestDeleteFileCallback());
    }

    private static class TestDeleteFileCallback implements DeleteFileCallback {

        @Override
        public void onFileDeleted() {
        }

        @Override
        public void onError(String message) {
            throw new IllegalStateException(message);
        }

    }

}
