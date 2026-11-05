package com.codingcat.changelogs.base.dialog;

import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog;
import com.codingcat.changelogs.platformapi.player.IPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static com.codingcat.changelogs.base.dialog.NativeComponents.toNative;
import static com.codingcat.changelogs.base.lang.TranslationSource.translatableManual;

public final class DialogPackets {
    public static void showDialog(@NotNull IPlayer player, @NotNull Dialog dialog, @NotNull PacketPhase packetPhase) {
        // The show/clear dialog packets are shared by both phases, the phase only decides when the dialog is first shown
        send(player, new ClientboundShowDialogPacket(Holder.direct(dialog)));
    }

    public static void showSimpleNotice(@NotNull IPlayer player, @NotNull String titleTranslation, @NotNull String bodyTranslation) {
        showSimpleNotice(player, titleTranslation, bodyTranslation, null, PacketPhase.PLAY);
    }

    public static void showSimpleConfirm(@NotNull IPlayer player, @NotNull String titleTranslation, @NotNull String bodyTranslation, @NotNull Action confirm, boolean confirmDanger, @NotNull Action cancel, @NotNull PacketPhase packetPhase) {
        CommonDialogData common = createSimpleDialog(player, titleTranslation, bodyTranslation, false, false);
        ActionButton confirmButton = new ActionButton(new CommonButtonData(toNative(translatableManual(player, "dialog.popup.confirm" + (confirmDanger ? "_danger" : ""))), Optional.empty(), 100), Optional.of(confirm));
        ActionButton cancelButton = new ActionButton(new CommonButtonData(toNative(translatableManual(player, "dialog.popup.cancel")), Optional.empty(), 100), Optional.of(cancel));
        showDialog(player, new MultiActionDialog(common, List.of(confirmButton, cancelButton), Optional.empty(), 2), packetPhase);
    }

    public static void showSimpleNotice(@NotNull IPlayer player, @NotNull String titleTranslation, @NotNull String bodyTranslation, @Nullable Action action, @NotNull PacketPhase packetPhase) {
        CommonDialogData common = createSimpleDialog(player, titleTranslation, bodyTranslation, action == null, true);
        ActionButton closeButton = new ActionButton(new CommonButtonData(toNative(translatableManual(player, "dialog.popup.close")), Optional.empty(), 40), Optional.ofNullable(action));
        showDialog(player, new NoticeDialog(common, closeButton), packetPhase);
    }

    public static @NotNull CommonDialogData createSimpleDialog(@NotNull IPlayer player, @NotNull String titleTranslation, @NotNull String bodyTranslation, boolean closeAfter, boolean canEscape) {
        DialogBody body = new PlainMessage(toNative(translatableManual(player, bodyTranslation)), ChangelogDialog.LINE_WIDTH);
        return new CommonDialogData(
                toNative(translatableManual(player, titleTranslation)),
                Optional.empty(), canEscape, false,
                closeAfter ? DialogAction.CLOSE : DialogAction.NONE, List.of(body), List.of()
        );
    }

    public static void clearDialog(@NotNull IPlayer player, @NotNull PacketPhase packetPhase) {
        send(player, ClientboundClearDialogPacket.INSTANCE);
    }

    private static void send(@NotNull IPlayer player, @NotNull Packet<?> packet) {
        ServerCommonPacketListenerImpl connection = switch (player.unwrapNative()) {
            case ServerPlayer p -> p.connection;
            case ServerCommonPacketListenerImpl c -> c;
            default -> throw new IllegalArgumentException("Cannot send packets to " + player.unwrapNative());
        };
        connection.send(packet);
    }

    public enum PacketPhase {
        CONFIGURATION,
        PLAY
    }
}
