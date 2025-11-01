package com.nexusbot.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.nexusbot.NexusBotMod;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

public class PlayerCommands {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // ========== COMANDO /g - ATIVA CHAT GLOBAL ==========
        dispatcher.register(Commands.literal("g")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();

                        // ✅ CORREÇÃO: Apenas muda para modo global
                        NexusBotMod.getInstance().getMonitorCore().getChatSystem().setGlobalMode(player);

                        // Mensagem de confirmação
                        player.sendMessage(new StringTextComponent("§6§l🌍 §6Chat Global §lATIVADO§6!"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7Agora suas mensagens serão enviadas para §etodo o servidor§7."), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7Use §b/l §7para voltar ao chat local."), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
                // /g <mensagem> - Envia mensagem global diretamente
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (source.getEntity() instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) source.getEntity();
                                String message = StringArgumentType.getString(context, "mensagem");

                                // Ativar modo global e enviar mensagem
                                NexusBotMod.getInstance().getMonitorCore().getChatSystem().setGlobalMode(player);
                                NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendGlobalMessage(player, message);
                                NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, "[GLOBAL] " + message);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );

        // ========== COMANDO /l - VOLTA PARA CHAT LOCAL ==========
        dispatcher.register(Commands.literal("l")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();
                        NexusBotMod.getInstance().getMonitorCore().getChatSystem().setLocalMode(player);

                        // Mensagem de confirmação
                        player.sendMessage(new StringTextComponent("§b§l🌎 §bChat Local §lATIVADO§b!"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7Agora suas mensagens serão enviadas apenas para jogadores em um raio de §b125 blocos§7."), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7Use §6/g §7para chat global."), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );

        // ========== COMANDO /s - CHAT DA STAFF ==========
        dispatcher.register(Commands.literal("s")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (source.getEntity() instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) source.getEntity();
                                String message = StringArgumentType.getString(context, "mensagem");

                                // Verificar se é OP
                                if (player.hasPermissions(2)) {
                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendStaffMessage(player, message);
                                    NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, "[STAFF] " + message);
                                } else {
                                    player.sendMessage(new StringTextComponent("§c§l🚫 §cVocê não tem permissão para usar o chat da staff!"), player.getUUID());
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );

        // ========== COMANDO /tell - MENSAGEM PRIVADA ==========
        dispatcher.register(Commands.literal("tell")
                .then(Commands.argument("jogador", StringArgumentType.string())
                        .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    if (source.getEntity() instanceof PlayerEntity) {
                                        PlayerEntity player = (PlayerEntity) source.getEntity();
                                        String target = StringArgumentType.getString(context, "jogador");
                                        String message = StringArgumentType.getString(context, "mensagem");

                                        NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendPrivateMessage(player, target, message);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
        );

        // ========== COMANDO /ajuda - TUTORIAL COMPLETO ==========
        dispatcher.register(Commands.literal("ajuda")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();

                        player.sendMessage(new StringTextComponent("§6§lNEXUSBOT - SISTEMA DE CHAT"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§e💬 Chat Automático:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- Digite normalmente §f→ §bChat Local §7(125 blocos)"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§6🌍 Comandos de Chat:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §6/g §f→ §6Ativa Chat Global"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §6/g <msg> §f→ §6Envia mensagem Global"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §b/l §f→ §bVolta para Chat Local"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §c/s <msg> §f→ §cChat da Staff §7(apenas OPs)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §d/tell <nick> <msg> §f→ Mensagem Privada"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§a🎫 Sistema de Tickets:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §a/ticket criar <msg> §f→ Criar ticket de ajuda"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §a/ticket status <id> §f→ Ver status do ticket"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §a/ticket listar §f→ Listar seus tickets"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§e📝 Outros Comandos:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §e/horario §f→ Ver horário do servidor"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §e/online §f→ Ver jogadores online"), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );

        // ========== COMANDO /ticket - SISTEMA COMPLETO ==========
        dispatcher.register(Commands.literal("ticket")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();

                        player.sendMessage(new StringTextComponent("§6§l🎫 SISTEMA DE TICKETS"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§e📋 Comandos Disponíveis:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§6/ticket criar <mensagem> §7- Criar novo ticket"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§6/ticket status <id> §7- Ver status do ticket"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§6/ticket listar §7- Listar seus tickets"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§a💡 Dica: §7Descreva seu problema com detalhes para agilizar o atendimento."), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
                // /ticket criar
                .then(Commands.literal("criar")
                        .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    if (source.getEntity() instanceof PlayerEntity) {
                                        PlayerEntity player = (PlayerEntity) source.getEntity();
                                        String message = StringArgumentType.getString(context, "mensagem");

                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().createTicket(player, message);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                // /ticket status
                .then(Commands.literal("status")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    if (source.getEntity() instanceof PlayerEntity) {
                                        PlayerEntity player = (PlayerEntity) source.getEntity();
                                        int ticketId = IntegerArgumentType.getInteger(context, "id");

                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().viewTicketStatus(player, ticketId);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                // /ticket listar
                .then(Commands.literal("listar")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (source.getEntity() instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) source.getEntity();
                                NexusBotMod.getInstance().getMonitorCore().getTicketSystem().listTickets(player, "");
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("filtro", StringArgumentType.string())
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    if (source.getEntity() instanceof PlayerEntity) {
                                        PlayerEntity player = (PlayerEntity) source.getEntity();
                                        String filter = StringArgumentType.getString(context, "filtro");

                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().listTickets(player, filter);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
        );

        // ========== COMANDO /horario ==========
        dispatcher.register(Commands.literal("horario")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();
                        long time = player.level.getDayTime();
                        long hour = (time / 1000 + 6) % 24;
                        long minute = (time % 1000) * 60 / 1000;
                        String timeStr = String.format("§6⏰ Horário: §e%02d:%02d", hour, minute);
                        player.sendMessage(new StringTextComponent(timeStr), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );

        // ========== COMANDO /online ==========
        dispatcher.register(Commands.literal("online")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();
                        String onlinePlayers = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getOnlinePlayersFormatted();
                        player.sendMessage(new StringTextComponent(onlinePlayers), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}