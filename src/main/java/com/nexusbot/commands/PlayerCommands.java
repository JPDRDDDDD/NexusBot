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

        // ========== COMANDO /g - ENVIA MENSAGEM GLOBAL DIRETAMENTE ==========
        dispatcher.register(Commands.literal("g")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (source.getEntity() instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) source.getEntity();
                                String message = StringArgumentType.getString(context, "mensagem");

                                // Envia mensagem global diretamente
                                NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendGlobalMessageDirect(player, message);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();

                        // Ativa modo global permanente
                        NexusBotMod.getInstance().getMonitorCore().getChatSystem().setGlobalMode(player);
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );

        // ========== COMANDO /l - ENVIA MENSAGEM LOCAL DIRETAMENTE ==========
        dispatcher.register(Commands.literal("l")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (source.getEntity() instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) source.getEntity();
                                String message = StringArgumentType.getString(context, "mensagem");

                                // Envia mensagem local diretamente
                                NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendLocalMessageDirect(player, message);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();

                        // Ativa modo local permanente
                        NexusBotMod.getInstance().getMonitorCore().getChatSystem().setLocalMode(player);
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

        // ========== COMANDO /r - RESPOSTA RÁPIDA ==========
        dispatcher.register(Commands.literal("r")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            if (source.getEntity() instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) source.getEntity();
                                String message = StringArgumentType.getString(context, "mensagem");

                                NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendReplyMessage(player, message);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();
                        player.sendMessage(new StringTextComponent("§c§l❌ §cUse: §f/r <mensagem> §cpara responder a última mensagem privada"), player.getUUID());
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );

        // ========== COMANDO /cores - MOSTRA CÓDIGOS DE CORES ==========
        dispatcher.register(Commands.literal("cores")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();
                        NexusBotMod.getInstance().getMonitorCore().getChatSystem().showColorCodes(player);
                    }
                    return Command.SINGLE_SUCCESS;
                })
        );

        // ========== COMANDO /ajuda - TUTORIAL COMPLETO ==========
        dispatcher.register(Commands.literal("ajuda")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (source.getEntity() instanceof PlayerEntity) {
                        PlayerEntity player = (PlayerEntity) source.getEntity();

                        player.sendMessage(new StringTextComponent("§6§lNEXUSBOT - SISTEMA DE CHAT"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§e💬 Sistema de Chat:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- Digite normalmente §f→ §6Chat Global §7(todo servidor)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- Use §b/l §f→ §bChat Local §7(125 blocos)"), player.getUUID());
                        player.sendMessage(new StringTextComponent(""), player.getUUID());
                        player.sendMessage(new StringTextComponent("§6🌍 Comandos de Chat:"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §6/g <msg> §f→ §6Envia mensagem global §7(apenas esta mensagem)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §6/g §f→ §6Ativa Chat Global §7(todas mensagens serão globais)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §b/l <msg> §f→ §bEnvia mensagem local §7(apenas esta mensagem)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §b/l §f→ §bAtiva Chat Local §7(todas mensagens serão locais)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §c/s <msg> §f→ §cChat da Staff §7(apenas OPs)"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §d/tell <nick> <msg> §f→ Mensagem Privada §a🔊"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §d/r <msg> §f→ Resposta Rápida §a🔊"), player.getUUID());
                        player.sendMessage(new StringTextComponent("§7- §a/cores §f→ Mostra códigos de cores"), player.getUUID());
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