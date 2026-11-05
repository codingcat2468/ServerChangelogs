package com.codingcat.changelogs.base.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeComponentsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void keepsTextColorAndPayloadlessCustomClick() {
        Key key = Key.key("server_changelogs", "dialog/changelog_view/start_editing.uid.3");
        Component adventure = Component.text("edit", NamedTextColor.RED).clickEvent(ClickEvent.custom(key, null));

        net.minecraft.network.chat.Component converted = NativeComponents.toNative(adventure);

        assertEquals("edit", converted.getString());
        var click = assertInstanceOf(net.minecraft.network.chat.ClickEvent.Custom.class, converted.getStyle().getClickEvent());
        assertEquals(Identifier.parse(key.asString()), click.id());
        assertTrue(click.payload().isEmpty());
    }
}
