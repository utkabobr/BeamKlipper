package ru.ytkab0bp.beamklipper.provider;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.R;

public class InstanceFilesProvider extends DocumentsProvider {
    // Use these as the default columns to return information about a root if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
    };

    // Use these as the default columns to return information about a document if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
    };

    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    private String getDocIdForFile(KlipperInstance instance, File file) {
        String path = file.getAbsolutePath();

        // Start at first char of path under root
        String rootPath = instance.getPublicDirectory().getPath();
        if (rootPath.equals(path)) {
            path = "";
        } else if (rootPath.endsWith("/")) {
            path = path.substring(rootPath.length());
        } else {
            path = path.substring(rootPath.length() + 1);
        }

        return "instance:" + instance.id + ":" + path;
    }

    private String getChildMimeTypes() {
        Set<String> mimeTypes = new HashSet<>();
        mimeTypes.add("image/*");
        mimeTypes.add("text/*");

        // Flatten the list into a string and insert newlines between the MIME type strings.
        StringBuilder mimeTypesString = new StringBuilder();
        for (String mimeType : mimeTypes) {
            mimeTypesString.append(mimeType).append("\n");
        }

        return mimeTypesString.toString();
    }

    private static String getTypeForName(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = name.substring(lastDot + 1);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    private KlipperInstance getInstanceForDocId(String docId) {
        if (docId.startsWith("instance:")) {
            String str = docId.substring("instance:".length());
            int i = str.indexOf(':');
            if (i == -1) return null;

            return KlipperInstance.getInstance(str.substring(0, i));
        }
        return null;
    }

    private File getFileForDocId(String docId) {
        if (docId.startsWith("instance:")) {
            String str = docId.substring("instance:".length());
            int i = str.indexOf(':');
            if (i == -1) return null;

            KlipperInstance inst = KlipperInstance.getInstance(str.substring(0, i));
            if (inst != null) {
                return new File(inst.getPublicDirectory(), str.substring(i + 1));
            }
        }
        return null;
    }

    private static String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint, CancellationSignal signal) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    private void includeFile(MatrixCursor result, String docId, KlipperInstance inst, File file)
            throws FileNotFoundException {
        if (docId == null) {
            docId = getDocIdForFile(inst, file);
        } else {
            file = getFileForDocId(docId);
        }

        int flags = 0;

        if (file.isDirectory()) {
            // Request the folder to lay out as a grid rather than a list. This also allows a larger
            // thumbnail to be displayed for each image.
            //            flags |= Document.FLAG_DIR_PREFERS_GRID;

            // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
            if (file.isDirectory() && file.canWrite()) {
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
            }
        } else if (file.canWrite()) {
            // If the file is writable set FLAG_SUPPORTS_WRITE and
            // FLAG_SUPPORTS_DELETE
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
        }

        String displayName = file.getName();
        String mimeType = getTypeForFile(file);

        if (mimeType.startsWith("image/")) {
            // Allow the image to be represented by a thumbnail rather than an icon
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.length());
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType);
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);

        // Add a custom icon
        row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.icon_static);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        for (KlipperInstance inst : KlipperInstance.getInstances()) {
            MatrixCursor.RowBuilder row = result.newRow();
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, inst.id);
            row.add(DocumentsContract.Root.COLUMN_SUMMARY, KlipperApp.INSTANCE.getString(R.string.InstanceN, inst.name));

            row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_CREATE);
            row.add(DocumentsContract.Root.COLUMN_TITLE, KlipperApp.INSTANCE.getString(R.string.AppName));

            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForFile(inst, inst.getPublicDirectory()));

            row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, getChildMimeTypes());
            row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, inst.getPublicDirectory().getFreeSpace());
            row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.icon_static);
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        File parent = getFileForDocId(parentDocumentId);
        KlipperInstance instance = getInstanceForDocId(parentDocumentId);
        for (File file : parent.listFiles()) {
            includeFile(result, null, instance, file);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        int accessMode = ParcelFileDescriptor.parseMode(mode);

        boolean isWrite = (mode.indexOf('w') != -1);
        if (isWrite) {
            try {
                Handler handler = new Handler(getContext().getMainLooper());
                return ParcelFileDescriptor.open(file, accessMode, handler, e -> {
                    if (e != null) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open document with id " + documentId + " and mode " + mode);
            }
        } else {
            return ParcelFileDescriptor.open(file, accessMode);
        }
    }

    @Override
    public String createDocument(String documentId, String mimeType, String displayName) throws FileNotFoundException {
        File parent = getFileForDocId(documentId);
        KlipperInstance inst = getInstanceForDocId(documentId);
        File file = new File(parent.getPath(), displayName);
        try {
            file.createNewFile();
            file.setWritable(true);
            file.setReadable(true);
        } catch (IOException e) {
            throw new FileNotFoundException("Failed to create document with name " +
                    displayName +" and documentId " + documentId);
        }
        return getDocIdForFile(inst, file);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        File file = getFileForDocId(documentId);
        if (!file.delete()) {
            throw new FileNotFoundException("Failed to delete document with id " + documentId);
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }
}
