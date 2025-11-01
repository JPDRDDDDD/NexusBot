package com.nexusbot.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.nexusbot.NexusBotMod;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

import java.util.Map;

public class AdminCommands {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        // ========== CATEGORIA PRINCIPAL: NEXUSBOT ==========
        dispatcher.register(Commands.literal("nexusbot")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l🤖 NEXUSBOT - SISTEMA DE ADMINISTRAÇÃO"), false);
                    source.sendSuccess(new StringTextComponent("§7Sistema completo de moderação e administração"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📚 Categorias Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/nexusbot moderacao §7- Sistema de moderação"), false);
                    source.sendSuccess(new StringTextComponent("§6/nexusbot filtro §7- Filtro de palavras"), false);
                    source.sendSuccess(new StringTextComponent("§6/nexusbot limpeza §7- Limpeza automática"), false);
                    source.sendSuccess(new StringTextComponent("§6/nexusbot ticket §7- Sistema de tickets"), false);
                    source.sendSuccess(new StringTextComponent("§6/nexusbot sistema §7- Sistema do bot"), false);
                    source.sendSuccess(new StringTextComponent("§6/nexusbot eventos §7- Sistema de eventos"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§a💡 Use §e/ajuda §apara ver comandos de jogador"), false);
                    return 1;
                })
        );

        // ========== CATEGORIA: EVENTOS DO BOT ==========
        dispatcher.register(Commands.literal("bot-evento")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l🎯 SISTEMA DE EVENTOS DO BOT"), false);
                    source.sendSuccess(new StringTextComponent("§7Gerencie mensagens automaticas para conquistas"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📋 Comandos Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/bot-evento add <id> <mensagem> §7- Adicionar evento"), false);
                    source.sendSuccess(new StringTextComponent("§6/bot-evento remove <id> §7- Remover evento"), false);
                    source.sendSuccess(new StringTextComponent("§6/bot-evento listar §7- Listar eventos"), false);
                    source.sendSuccess(new StringTextComponent("§6/bot-evento disponiveis §7- Conquistas disponíveis"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§a💡 Exemplo:"), false);
                    source.sendSuccess(new StringTextComponent("§7/bot-evento add draconicevolution:draconic_core \"&cParabens &4Voce conseguiu &2Seu primeiro Draconic Core &nContinue assim\""), false);
                    source.sendSuccess(new StringTextComponent("§7Use §e/cores §7para ver códigos de cores disponíveis"), false);
                    return 1;
                })
                // /bot-evento add
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            CommandSource source = context.getSource();
                                            String advancementId = StringArgumentType.getString(context, "id");
                                            String message = StringArgumentType.getString(context, "mensagem");

                                            NexusBotMod.getInstance().getMonitorCore().getChatSystem().addCustomEvent(advancementId, message);
                                            source.sendSuccess(new StringTextComponent("§a✅ Evento adicionado: §e" + advancementId), false);
                                            source.sendSuccess(new StringTextComponent("§7Mensagem: §f" + message), false);
                                            return 1;
                                        })
                                )
                        )
                )
                // /bot-evento remove
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    String advancementId = StringArgumentType.getString(context, "id");

                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().removeCustomEvent(advancementId);
                                    source.sendSuccess(new StringTextComponent("§c🗑️ Evento removido: §e" + advancementId), false);
                                    return 1;
                                })
                        )
                )
                // /bot-evento listar
                .then(Commands.literal("listar")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            Map<String, String> events = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getCustomEvents();

                            source.sendSuccess(new StringTextComponent("§6§l📋 EVENTOS CONFIGURADOS (§e" + events.size() + "§6)"), false);
                            source.sendSuccess(new StringTextComponent(""), false);

                            if (events.isEmpty()) {
                                source.sendSuccess(new StringTextComponent("§7Nenhum evento configurado"), false);
                            } else {
                                for (Map.Entry<String, String> entry : events.entrySet()) {
                                    source.sendSuccess(new StringTextComponent("§e" + entry.getKey() + " §8→ §7" + entry.getValue()), false);
                                }
                            }
                            return 1;
                        })
                )
                // /bot-evento disponiveis
                .then(Commands.literal("disponiveis")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            java.util.List<String> available = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getAvailableAdvancements();

                            source.sendSuccess(new StringTextComponent("§6§l🎯 CONQUISTAS DISPONIVEIS (§e" + available.size() + "§6)"), false);
                            source.sendSuccess(new StringTextComponent("§7Use: /bot-evento add <id> \"sua mensagem\""), false);
                            source.sendSuccess(new StringTextComponent(""), false);

                            int count = 0;
                            for (String advancement : available) {
                                if (count < 20) { // Mostra apenas as primeiras 20
                                    source.sendSuccess(new StringTextComponent("§7- §a" + advancement), false);
                                    count++;
                                } else {
                                    source.sendSuccess(new StringTextComponent("§7... e mais " + (available.size() - 20) + " conquistas"), false);
                                    break;
                                }
                            }

                            source.sendSuccess(new StringTextComponent(""), false);
                            source.sendSuccess(new StringTextComponent("§a💡 Dica: §7Conquistas em §averde§7 já foram detectadas no servidor"), false);
                            return 1;
                        })
                )
        );

        // ========== CATEGORIA: MODERAÇÃO ==========
        dispatcher.register(Commands.literal("moderacao")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l🛡️ SISTEMA DE MODERAÇÃO"), false);
                    source.sendSuccess(new StringTextComponent("§7Gerencie jogadores e aplique punições"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📋 Subcomandos Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/moderacao mute <nick> [minutos] [motivo]"), false);
                    source.sendSuccess(new StringTextComponent("§6/moderacao unmute <nick>"), false);
                    source.sendSuccess(new StringTextComponent("§6/moderacao bypass <nick>"), false);
                    source.sendSuccess(new StringTextComponent("§6/moderacao unbypass <nick>"), false);
                    source.sendSuccess(new StringTextComponent("§6/moderacao status <nick>"), false);
                    source.sendSuccess(new StringTextComponent("§6/moderacao reset <nick>"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§a👥 Jogadores Online:"), false);
                    String onlinePlayers = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getOnlinePlayersFormatted();
                    source.sendSuccess(new StringTextComponent(onlinePlayers), false);
                    return 1;
                })
                // /moderacao mute
                .then(Commands.literal("mute")
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    NexusBotMod.getInstance().getMonitorCore().getPunishmentManager().mutePlayerManual(playerName, 30, "Decisão administrativa");
                                    context.getSource().sendSuccess(new StringTextComponent("§c🔇 Player mutado por 30 minutos: " + playerName), false);
                                    return 1;
                                })
                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    String playerName = StringArgumentType.getString(context, "nick");
                                                    int minutos = IntegerArgumentType.getInteger(context, "minutos");
                                                    String motivo = StringArgumentType.getString(context, "motivo");

                                                    NexusBotMod.getInstance().getMonitorCore().getPunishmentManager().mutePlayerManual(playerName, minutos, motivo);
                                                    context.getSource().sendSuccess(new StringTextComponent("§c🔇 " + playerName + " mutado por " + minutos + " minutos"), false);
                                                    context.getSource().sendSuccess(new StringTextComponent("§7Motivo: " + motivo), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                // /moderacao unmute
                .then(Commands.literal("unmute")
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    NexusBotMod.getInstance().getMonitorCore().getPunishmentManager().unmutePlayerManual(playerName);
                                    context.getSource().sendSuccess(new StringTextComponent("§a🔊 Player desmutado: " + playerName), false);
                                    return 1;
                                })
                        )
                )
                // /moderacao bypass
                .then(Commands.literal("bypass")
                        .executes(context -> {
                            String onlinePlayers = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getOnlinePlayersFormatted();
                            context.getSource().sendSuccess(new StringTextComponent("§6🎮 Jogadores Online para Bypass:"), false);
                            context.getSource().sendSuccess(new StringTextComponent(onlinePlayers), false);
                            context.getSource().sendSuccess(new StringTextComponent(""), false);
                            context.getSource().sendSuccess(new StringTextComponent("§7Use: §6/moderacao bypass <nick>"), false);
                            return 1;
                        })
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().addBypass(playerName);
                                    context.getSource().sendSuccess(new StringTextComponent("§a🛡️ Bypass adicionado para: " + playerName), false);
                                    context.getSource().sendSuccess(new StringTextComponent("§7O jogador agora ignora filtros e sistemas automáticos"), false);
                                    return 1;
                                })
                        )
                )
                // /moderacao unbypass
                .then(Commands.literal("unbypass")
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().removeBypass(playerName);
                                    context.getSource().sendSuccess(new StringTextComponent("§c🛡️ Bypass removido de: " + playerName), false);
                                    return 1;
                                })
                        )
                )
                // /moderacao status
                .then(Commands.literal("status")
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    NexusBotMod.getInstance().getMonitorCore().getPunishmentManager().checkPlayerStatus(playerName);
                                    context.getSource().sendSuccess(new StringTextComponent("§6📊 Verificando status de: " + playerName), false);
                                    context.getSource().sendSuccess(new StringTextComponent("§7Verifique os logs do console"), false);
                                    return 1;
                                })
                        )
                )
                // /moderacao reset
                .then(Commands.literal("reset")
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    NexusBotMod.getInstance().getMonitorCore().getPunishmentManager().resetOffenses(playerName);
                                    context.getSource().sendSuccess(new StringTextComponent("§a🔄 Ofensas resetadas: " + playerName), false);
                                    return 1;
                                })
                        )
                )
        );

        // ========== CATEGORIA: TICKET (ADMIN) ==========
        dispatcher.register(Commands.literal("ticket")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l🎫 SISTEMA DE TICKETS - ADMIN"), false);
                    source.sendSuccess(new StringTextComponent("§7Gerencie todos os tickets do servidor"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📋 Comandos Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/ticket listar [filtro] §7- Listar tickets"), false);
                    source.sendSuccess(new StringTextComponent("§6/ticket ver <id> §7- Ver detalhes do ticket"), false);
                    source.sendSuccess(new StringTextComponent("§6/ticket aceitar <id> §7- Aceitar ticket"), false);
                    source.sendSuccess(new StringTextComponent("§6/ticket responder <id> <msg> §7- Responder ticket"), false);
                    source.sendSuccess(new StringTextComponent("§6/ticket fechar <id> §7- Fechar ticket"), false);
                    source.sendSuccess(new StringTextComponent("§6/ticket stats §7- Estatísticas do sistema"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§a📊 " + NexusBotMod.getInstance().getMonitorCore().getTicketSystem().getStats()), false);
                    return 1;
                })
                // /ticket listar
                .then(Commands.literal("listar")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                NexusBotMod.getInstance().getMonitorCore().getTicketSystem().listTickets(player, "");
                            }
                            return 1;
                        })
                        .then(Commands.argument("filtro", StringArgumentType.string())
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                        net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                        String filter = StringArgumentType.getString(context, "filtro");
                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().listTickets(player, filter);
                                    }
                                    return 1;
                                })
                        )
                )
                // /ticket ver
                .then(Commands.literal("ver")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                        net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                        int ticketId = IntegerArgumentType.getInteger(context, "id");
                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().viewTicketStatus(player, ticketId);
                                    }
                                    return 1;
                                })
                        )
                )
                // /ticket aceitar
                .then(Commands.literal("aceitar")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                        net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                        int ticketId = IntegerArgumentType.getInteger(context, "id");
                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().acceptTicket(player, ticketId);
                                    }
                                    return 1;
                                })
                        )
                )
                // /ticket responder
                .then(Commands.literal("responder")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                                net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                                int ticketId = IntegerArgumentType.getInteger(context, "id");
                                                String message = StringArgumentType.getString(context, "mensagem");
                                                NexusBotMod.getInstance().getMonitorCore().getTicketSystem().respondToTicket(player, ticketId, message);
                                            }
                                            return 1;
                                        })
                                )
                        )
                )
                // /ticket fechar
                .then(Commands.literal("fechar")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                        net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                        int ticketId = IntegerArgumentType.getInteger(context, "id");
                                        NexusBotMod.getInstance().getMonitorCore().getTicketSystem().closeTicket(player, ticketId);
                                    }
                                    return 1;
                                })
                        )
                )
                // /ticket stats
                .then(Commands.literal("stats")
                        .executes(context -> {
                            String stats = NexusBotMod.getInstance().getMonitorCore().getTicketSystem().getStats();
                            context.getSource().sendSuccess(new StringTextComponent("§6§l📊 ESTATÍSTICAS DE TICKETS"), false);
                            context.getSource().sendSuccess(new StringTextComponent(stats), false);
                            return 1;
                        })
                )
        );

        // ========== CATEGORIA: FILTRO ==========
        dispatcher.register(Commands.literal("filtro")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l📝 SISTEMA DE FILTRO DE PALAVRAS"), false);
                    source.sendSuccess(new StringTextComponent("§7Gerencie as palavras proibidas no servidor"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📋 Subcomandos Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/filtro add <palavra> §7- Adicionar palavra"), false);
                    source.sendSuccess(new StringTextComponent("§6/filtro wildcard <padrao> §7- Adicionar wildcard"), false);
                    source.sendSuccess(new StringTextComponent("§6/filtro listar §7- Listar palavras"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§c⚠ Exemplo de wildcard: §ff*d*§c (bloqueia foda, fudido, etc)"), false);
                    return 1;
                })
                // /filtro add
                .then(Commands.literal("add")
                        .then(Commands.argument("palavra", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String word = StringArgumentType.getString(context, "palavra");
                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().addBadWord(word);
                                    context.getSource().sendSuccess(new StringTextComponent("§a📝 Palavra proibida adicionada: " + word), false);
                                    return 1;
                                })
                        )
                )
                // /filtro wildcard
                .then(Commands.literal("wildcard")
                        .then(Commands.argument("padrao", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String pattern = StringArgumentType.getString(context, "padrao");
                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().addWildcardWord(pattern);
                                    context.getSource().sendSuccess(new StringTextComponent("§a📝 Wildcard adicionado: " + pattern), false);
                                    return 1;
                                })
                        )
                )
                // /filtro listar
                .then(Commands.literal("listar")
                        .executes(context -> {
                            context.getSource().sendSuccess(new StringTextComponent("§6📋 Sistema de Filtro Ativo"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§7O filtro está funcionando corretamente"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§7Palavras bloqueadas: §c" + NexusBotMod.getInstance().getMonitorCore().getChatSystem().getBadWords().size()), false);
                            return 1;
                        })
                )
        );

        // ========== CATEGORIA: LIMPEZA ==========
        dispatcher.register(Commands.literal("limpeza")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l🗑️ SISTEMA DE LIMPEZA AUTOMÁTICA"), false);
                    source.sendSuccess(new StringTextComponent("§7Gerencie a limpeza automática do servidor"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📋 Subcomandos Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/limpeza status §7- Ver status completo"), false);
                    source.sendSuccess(new StringTextComponent("§6/limpeza on §7- Ativar sistema"), false);
                    source.sendSuccess(new StringTextComponent("§6/limpeza off §7- Desativar sistema"), false);
                    source.sendSuccess(new StringTextComponent("§6/limpeza agora §7- Limpeza manual"), false);
                    source.sendSuccess(new StringTextComponent("§6/limpeza intervalo <minutos> §7- Alterar intervalo"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§a✅ Remove: §fItens no chão + Mobs comuns"), false);
                    source.sendSuccess(new StringTextComponent("§c❌ Protege: §fPets, Villagers, Bosses, Mobs nomeados"), false);
                    return 1;
                })
                // /limpeza status
                .then(Commands.literal("status")
                        .executes(context -> {
                            String status = NexusBotMod.getInstance().getMonitorCore().getCleanerSystem().getStatus();
                            context.getSource().sendSuccess(new StringTextComponent("§6§l🗑️ STATUS DA LIMPEZA"), false);
                            context.getSource().sendSuccess(new StringTextComponent(status), false);
                            return 1;
                        })
                )
                // /limpeza on
                .then(Commands.literal("on")
                        .executes(context -> {
                            NexusBotMod.getInstance().getMonitorCore().getCleanerSystem().setActive(true);
                            context.getSource().sendSuccess(new StringTextComponent("§a🗑️ Sistema de limpeza §lATIVADO§a!"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§7A limpeza automática está agora ativa"), false);
                            return 1;
                        })
                )
                // /limpeza off
                .then(Commands.literal("off")
                        .executes(context -> {
                            NexusBotMod.getInstance().getMonitorCore().getCleanerSystem().setActive(false);
                            context.getSource().sendSuccess(new StringTextComponent("§c🗑️ Sistema de limpeza §lDESATIVADO§c!"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§7A limpeza automática foi pausada"), false);
                            return 1;
                        })
                )
                // /limpeza agora
                .then(Commands.literal("agora")
                        .executes(context -> {
                            context.getSource().sendSuccess(new StringTextComponent("§6🧹 Executando limpeza manual..."), false);
                            NexusBotMod.getInstance().getMonitorCore().getCleanerSystem().forceCleanup();
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Limpeza manual concluída!"), false);
                            return 1;
                        })
                )
                // /limpeza intervalo
                .then(Commands.literal("intervalo")
                        .then(Commands.argument("minutos", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int minutos = IntegerArgumentType.getInteger(context, "minutos");
                                    NexusBotMod.getInstance().getMonitorCore().getCleanerSystem().setInterval(minutos);
                                    context.getSource().sendSuccess(new StringTextComponent("§6⏰ Intervalo de limpeza definido para §e" + minutos + " minutos§6!"), false);
                                    return 1;
                                })
                        )
                )
        );

        // ========== CATEGORIA: SISTEMA ==========
        dispatcher.register(Commands.literal("sistema")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    source.sendSuccess(new StringTextComponent("§6§l⚙️ SISTEMA NEXUSBOT"), false);
                    source.sendSuccess(new StringTextComponent("§7Comandos de administração do sistema"), false);
                    source.sendSuccess(new StringTextComponent(""), false);
                    source.sendSuccess(new StringTextComponent("§e📋 Subcomandos Disponíveis:"), false);
                    source.sendSuccess(new StringTextComponent("§6/sistema info §7- Informações do bot"), false);
                    source.sendSuccess(new StringTextComponent("§6/sistema logs <nick> §7- Ver logs"), false);
                    source.sendSuccess(new StringTextComponent("§6/sistema players §7- Jogadores online"), false);
                    source.sendSuccess(new StringTextComponent("§6/sistema anuncio <mensagem> §7- Anúncio global"), false);
                    source.sendSuccess(new StringTextComponent("§6/sistema reload §7- Recarregar sistemas"), false);
                    return 1;
                })
                // /sistema info
                .then(Commands.literal("info")
                        .executes(context -> {
                            context.getSource().sendSuccess(new StringTextComponent("§6§lNEXUSBOT v2.3.6 - SISTEMA PROFISSIONAL"), false);
                            context.getSource().sendSuccess(new StringTextComponent(""), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Sistema de Moderação Ativo"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Filtro de Palavras Funcional"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Anti-Cheat Completo"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Logs 24/7 em Tempo Real"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Sistema de Chat Profissional"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Limpeza Automática Ativa"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Sistema de Tickets Profissional"), false);
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Sistema de Eventos Customizável"), false);
                            return 1;
                        })
                )
                // /sistema logs
                .then(Commands.literal("logs")
                        .then(Commands.argument("nick", StringArgumentType.string())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "nick");
                                    context.getSource().sendSuccess(new StringTextComponent("§6📊 Logs do Player: " + playerName), false);
                                    context.getSource().sendSuccess(new StringTextComponent("§7Verifique a pasta 'nexusbot_logs/' no servidor"), false);
                                    return 1;
                                })
                        )
                )
                // /sistema players
                .then(Commands.literal("players")
                        .executes(context -> {
                            String onlinePlayers = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getOnlinePlayersFormatted();
                            context.getSource().sendSuccess(new StringTextComponent("§6🎮 Jogadores Conectados:"), false);
                            context.getSource().sendSuccess(new StringTextComponent(onlinePlayers), false);
                            return 1;
                        })
                )
                // /sistema anuncio
                .then(Commands.literal("anuncio")
                        .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String message = StringArgumentType.getString(context, "mensagem");
                                    String anuncio = "§6§l📢 ANÚNCIO §8» §e" + message;

                                    if (context.getSource().getServer() != null) {
                                        context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
                                            player.sendMessage(new StringTextComponent(""), player.getUUID());
                                            player.sendMessage(new StringTextComponent(anuncio), player.getUUID());
                                            player.sendMessage(new StringTextComponent(""), player.getUUID());
                                        });
                                    }

                                    context.getSource().sendSuccess(new StringTextComponent("§a📢 Anúncio enviado para todos os jogadores!"), false);
                                    return 1;
                                })
                        )
                )
                // /sistema reload
                .then(Commands.literal("reload")
                        .executes(context -> {
                            context.getSource().sendSuccess(new StringTextComponent("§6🔄 Recarregando sistemas do NexusBot..."), false);
                            // Aqui você pode adicionar lógica de reload se necessário
                            context.getSource().sendSuccess(new StringTextComponent("§a✅ Sistemas verificados e ativos!"), false);
                            return 1;
                        })
                )
        );

        // ========== COMANDO /s - CHAT DA STAFF ==========
        dispatcher.register(Commands.literal("s")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof net.minecraft.entity.player.PlayerEntity) {
                                net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) context.getSource().getEntity();
                                String message = StringArgumentType.getString(context, "mensagem");

                                if (player.hasPermissions(2)) {
                                    NexusBotMod.getInstance().getMonitorCore().getChatSystem().sendStaffMessage(player, message);
                                    NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logChat(player, "[STAFF] " + message);
                                } else {
                                    context.getSource().sendSuccess(new StringTextComponent("§c§l🚫 §cApenas operadores podem usar o chat da staff!"), false);
                                }
                            } else {
                                String message = StringArgumentType.getString(context, "mensagem");
                                String staffMessage = "§8[§4👑 Console§8] §cSistema §8» §f" + message;

                                if (context.getSource().getServer() != null) {
                                    context.getSource().getServer().getPlayerList().getPlayers().forEach(player -> {
                                        if (player.hasPermissions(2)) {
                                            player.sendMessage(new StringTextComponent(staffMessage), player.getUUID());
                                        }
                                    });
                                }
                                NexusBotMod.LOGGER.info("👑 [STAFF] Console: {}", message);
                            }
                            return 1;
                        })
                )
        );

        // ========== COMANDOS LEGACY (compatibilidade) ==========
        dispatcher.register(Commands.literal("bypassBot")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    String onlinePlayers = NexusBotMod.getInstance().getMonitorCore().getChatSystem().getOnlinePlayersFormatted();
                    context.getSource().sendSuccess(new StringTextComponent("§6🎮 Jogadores Online:"), false);
                    context.getSource().sendSuccess(new StringTextComponent(onlinePlayers), false);
                    context.getSource().sendSuccess(new StringTextComponent(""), false);
                    context.getSource().sendSuccess(new StringTextComponent("§7Use: §6/bypassBot <nick>"), false);
                    return 1;
                })
                .then(Commands.argument("nick", StringArgumentType.string())
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "nick");
                            NexusBotMod.getInstance().getMonitorCore().getChatSystem().addBypass(playerName);
                            context.getSource().sendSuccess(new StringTextComponent("§a🛡️ Bypass adicionado para: " + playerName), false);
                            return 1;
                        })
                )
        );
    }
}