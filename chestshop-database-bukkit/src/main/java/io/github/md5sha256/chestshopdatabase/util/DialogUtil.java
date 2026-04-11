package io.github.md5sha256.chestshopdatabase.util;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DialogUtil {

    public static final ClickCallback.Options DEFAULT_CALLBACK_OPTIONS =
            ClickCallback.Options.builder().build();

    public static final DialogAction CLOSE_DIALOG_ACTION =
            DialogAction.customClick(DialogUtil::closeDialog, DEFAULT_CALLBACK_OPTIONS);

    public static final DialogBase EMPTY_DIALOG_BASE = DialogBase
            .builder(Component.empty())
            .build();

    public static final Dialog EMPTY_DIALOG = Dialog.create(reg -> reg.empty()
            .base(EMPTY_DIALOG_BASE)
            .type(DialogType.notice()));

    public static void closeDialog(@NotNull DialogResponseView view, @NotNull Audience audience) {
        audience.closeDialog();
    }

    public static DialogAction openDialogAction(@NotNull Supplier<Dialog> dialogSupplier) {
        return DialogAction.customClick((view, audience) ->
                        audience.showDialog(dialogSupplier.get())
                , DEFAULT_CALLBACK_OPTIONS);
    }

}
