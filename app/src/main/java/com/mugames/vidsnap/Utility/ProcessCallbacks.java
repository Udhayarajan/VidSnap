package com.mugames.vidsnap.Utility;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mugames.vidsnap.PopUpDialog;

public interface ProcessCallbacks {
    void onErrorOccurred(String reasonToDisplay,Exception exception);
    PopUpDialog getDialog();
}
