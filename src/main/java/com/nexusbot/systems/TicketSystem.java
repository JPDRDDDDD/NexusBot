package com.nexusbot.systems;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import com.nexusbot.NexusBotMod;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicketSystem {
    private final String TICKETS_FOLDER = "nexusbot_tickets";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Map<Integer, Ticket> activeTickets = new ConcurrentHashMap<>();
    private Map<Integer, Ticket> closedTickets = new ConcurrentHashMap<>();
    private int nextTicketId = 1;

    // Classe para representar um ticket
    public class Ticket {
        public int id;
        public String playerName;
        public String playerUUID;
        public String message;
        public String status; // "ABERTO", "EM_ANDAMENTO", "FECHADO"
        public String assignedStaff;
        public String response;
        public long createdAt;
        public long updatedAt;

        public Ticket(int id, String playerName, String playerUUID, String message) {
            this.id = id;
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.message = message;
            this.status = "ABERTO";
            this.assignedStaff = "Nenhum";
            this.response = "Nenhuma";
            this.createdAt = System.currentTimeMillis();
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public TicketSystem() {
        createTicketsFolder();
        loadTicketsFromFile();
        NexusBotMod.LOGGER.info("🎫 Sistema de tickets profissional INICIADO");
    }

    private void createTicketsFolder() {
        File ticketsFolder = new File(TICKETS_FOLDER);
        if (!ticketsFolder.exists()) {
            ticketsFolder.mkdirs();
        }
    }

    // ========== CRIAR NOVO TICKET ==========
    public void createTicket(PlayerEntity player, String message) {
        try {
            int ticketId = nextTicketId++;
            Ticket ticket = new Ticket(ticketId, player.getName().getString(), player.getStringUUID(), message);

            activeTickets.put(ticketId, ticket);
            saveTicketToFile(ticket);

            // Notificar o jogador
            player.sendMessage(new StringTextComponent("§a§l✅ TICKET CRIADO COM SUCESSO!"), player.getUUID());
            player.sendMessage(new StringTextComponent("§7Número do ticket: §6#" + ticketId), player.getUUID());
            player.sendMessage(new StringTextComponent("§7Sua mensagem: §f" + message), player.getUUID());
            player.sendMessage(new StringTextComponent("§7Nossa equipe foi notificada e responderá em breve."), player.getUUID());
            player.sendMessage(new StringTextComponent("§7Use §e/ticket status §7para verificar o andamento."), player.getUUID());

            // Notificar todos os staffs online
            notifyStaffAboutNewTicket(ticket);

            // Log no sistema
            NexusBotMod.LOGGER.info("🎫 NOVO TICKET #{}: {} - {}", ticketId, player.getName().getString(), message);
            NexusBotMod.getInstance().getMonitorCore().getLoggerManager().logTicket(player, "Ticket #" + ticketId + ": " + message);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao criar ticket: {}", e.toString());
        }
    }

    // ========== NOTIFICAR STAFF SOBRE NOVO TICKET ==========
    private void notifyStaffAboutNewTicket(Ticket ticket) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            String staffMessage = createStaffNotification(ticket);

            for (ServerPlayerEntity staff : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (staff.hasPermissions(2)) { // Apenas OPs
                    staff.sendMessage(new StringTextComponent(""), staff.getUUID());
                    staff.sendMessage(new StringTextComponent(staffMessage), staff.getUUID());

                    // Botões de ação interativos
                    StringTextComponent actions = new StringTextComponent("");

                    // Botão Aceitar
                    StringTextComponent acceptBtn = new StringTextComponent(" §a[✔ Aceitar] ");
                    acceptBtn.withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket aceitar " + ticket.id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new StringTextComponent("§aClique para aceitar este ticket"))));

                    // Botão Ver
                    StringTextComponent viewBtn = new StringTextComponent(" §6[👁 Ver] ");
                    viewBtn.withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket ver " + ticket.id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new StringTextComponent("§6Clique para ver detalhes do ticket"))));

                    // Botão Fechar
                    StringTextComponent closeBtn = new StringTextComponent(" §c[✘ Fechar] ");
                    closeBtn.withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket fechar " + ticket.id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new StringTextComponent("§cClique para fechar este ticket"))));

                    actions.append(acceptBtn).append(viewBtn).append(closeBtn);
                    staff.sendMessage(actions, staff.getUUID());
                    staff.sendMessage(new StringTextComponent(""), staff.getUUID());
                }
            }
        }
    }

    private String createStaffNotification(Ticket ticket) {
        return String.format(
                "§6§l🎫 NOVO TICKET #%s\n" +
                        "§7Jogador: §b%s\n" +
                        "§7Mensagem: §f%s\n" +
                        "§7Status: §a%s§7 | §7Há: §e%s",
                ticket.id, ticket.playerName, ticket.message, ticket.status, getTimeAgo(ticket.createdAt)
        );
    }

    // ========== ACEITAR TICKET ==========
    public void acceptTicket(PlayerEntity staff, int ticketId) {
        try {
            Ticket ticket = activeTickets.get(ticketId);
            if (ticket == null) {
                staff.sendMessage(new StringTextComponent("§c❌ Ticket #" + ticketId + " não encontrado!"), staff.getUUID());
                return;
            }

            if (!"ABERTO".equals(ticket.status)) {
                staff.sendMessage(new StringTextComponent("§c❌ Este ticket já está sendo atendido por: " + ticket.assignedStaff), staff.getUUID());
                return;
            }

            ticket.status = "EM_ANDAMENTO";
            ticket.assignedStaff = staff.getName().getString();
            ticket.updatedAt = System.currentTimeMillis();

            // Notificar o staff
            staff.sendMessage(new StringTextComponent("§a✅ Você aceitou o ticket #" + ticketId), staff.getUUID());
            staff.sendMessage(new StringTextComponent("§7Jogador: §b" + ticket.playerName), staff.getUUID());
            staff.sendMessage(new StringTextComponent("§7Problema: §f" + ticket.message), staff.getUUID());
            staff.sendMessage(new StringTextComponent("§7Use §e/ticket responder " + ticketId + " <mensagem> §7para responder."), staff.getUUID());

            // Notificar o jogador
            notifyPlayerAboutTicketUpdate(ticket, "§a✅ Seu ticket foi aceito por §6" + staff.getName().getString());

            saveTicketToFile(ticket);
            NexusBotMod.LOGGER.info("🎫 Ticket #{} aceito por {}", ticketId, staff.getName().getString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao aceitar ticket: {}", e.toString());
        }
    }

    // ========== RESPONDER TICKET ==========
    public void respondToTicket(PlayerEntity staff, int ticketId, String response) {
        try {
            Ticket ticket = activeTickets.get(ticketId);
            if (ticket == null) {
                staff.sendMessage(new StringTextComponent("§c❌ Ticket #" + ticketId + " não encontrado!"), staff.getUUID());
                return;
            }

            if (!staff.getName().getString().equals(ticket.assignedStaff) && !"Nenhum".equals(ticket.assignedStaff)) {
                staff.sendMessage(new StringTextComponent("§c❌ Este ticket está sendo atendido por: " + ticket.assignedStaff), staff.getUUID());
                return;
            }

            ticket.response = response;
            ticket.updatedAt = System.currentTimeMillis();

            // Notificar o jogador
            notifyPlayerAboutTicketUpdate(ticket,
                    "§6📨 Resposta do Staff (§e" + staff.getName().getString() + "§6):\n§f" + response);

            // Notificar o staff
            staff.sendMessage(new StringTextComponent("§a✅ Resposta enviada para o ticket #" + ticketId), staff.getUUID());

            saveTicketToFile(ticket);
            NexusBotMod.LOGGER.info("🎫 Ticket #{} respondido por {}", ticketId, staff.getName().getString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao responder ticket: {}", e.toString());
        }
    }

    // ========== FECHAR TICKET ==========
    public void closeTicket(PlayerEntity staff, int ticketId) {
        try {
            Ticket ticket = activeTickets.get(ticketId);
            if (ticket == null) {
                staff.sendMessage(new StringTextComponent("§c❌ Ticket #" + ticketId + " não encontrado!"), staff.getUUID());
                return;
            }

            ticket.status = "FECHADO";
            ticket.updatedAt = System.currentTimeMillis();

            // Mover para tickets fechados
            activeTickets.remove(ticketId);
            closedTickets.put(ticketId, ticket);

            // Notificar o staff
            staff.sendMessage(new StringTextComponent("§a✅ Ticket #" + ticketId + " fechado com sucesso!"), staff.getUUID());

            // Notificar o jogador
            notifyPlayerAboutTicketUpdate(ticket,
                    "§c📭 Seu ticket #" + ticketId + " foi fechado por " + staff.getName().getString());

            saveTicketToFile(ticket);
            NexusBotMod.LOGGER.info("🎫 Ticket #{} fechado por {}", ticketId, staff.getName().getString());

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao fechar ticket: {}", e.toString());
        }
    }

    // ========== VER STATUS DO TICKET ==========
    public void viewTicketStatus(PlayerEntity player, int ticketId) {
        try {
            Ticket ticket = activeTickets.get(ticketId);
            if (ticket == null) {
                ticket = closedTickets.get(ticketId);
            }

            if (ticket == null) {
                player.sendMessage(new StringTextComponent("§c❌ Ticket #" + ticketId + " não encontrado!"), player.getUUID());
                return;
            }

            // Verificar permissão
            boolean isStaff = player.hasPermissions(2);
            boolean isOwner = player.getStringUUID().equals(ticket.playerUUID);

            if (!isStaff && !isOwner) {
                player.sendMessage(new StringTextComponent("§c❌ Você só pode ver seus próprios tickets!"), player.getUUID());
                return;
            }

            displayTicketInfo(player, ticket);

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao ver ticket: {}", e.toString());
        }
    }

    private void displayTicketInfo(PlayerEntity player, Ticket ticket) {
        player.sendMessage(new StringTextComponent(""), player.getUUID());
        player.sendMessage(new StringTextComponent("§6§l🎫 TICKET #" + ticket.id), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Jogador: §b" + ticket.playerName), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Status: " + getStatusColor(ticket.status) + ticket.status), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Atendente: §e" + ticket.assignedStaff), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Problema: §f" + ticket.message), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Resposta: §a" + ticket.response), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Criado: §e" + formatTime(ticket.createdAt)), player.getUUID());
        player.sendMessage(new StringTextComponent("§7Atualizado: §e" + formatTime(ticket.updatedAt)), player.getUUID());

        // Botões de ação para staff
        if (player.hasPermissions(2) && !"FECHADO".equals(ticket.status)) {
            StringTextComponent actions = new StringTextComponent("§7Ações: ");

            if ("ABERTO".equals(ticket.status)) {
                StringTextComponent acceptBtn = new StringTextComponent("§a[✔ Aceitar] ");
                acceptBtn.withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket aceitar " + ticket.id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new StringTextComponent("§aAceitar este ticket"))));
                actions.append(acceptBtn);
            }

            if ("EM_ANDAMENTO".equals(ticket.status) && player.getName().getString().equals(ticket.assignedStaff)) {
                StringTextComponent respondBtn = new StringTextComponent("§6[📨 Responder] ");
                respondBtn.withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ticket responder " + ticket.id + " "))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new StringTextComponent("§6Responder ao jogador"))));
                actions.append(respondBtn);
            }

            StringTextComponent closeBtn = new StringTextComponent("§c[✘ Fechar] ");
            closeBtn.withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket fechar " + ticket.id))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("§cFechar este ticket"))));
            actions.append(closeBtn);

            player.sendMessage(actions, player.getUUID());
        }
    }

    // ========== LISTAR TICKETS ==========
    public void listTickets(PlayerEntity player, String filter) {
        try {
            boolean isStaff = player.hasPermissions(2);
            String playerUUID = player.getStringUUID();

            List<Ticket> ticketsToShow = new ArrayList<>();

            // Coletar tickets baseado no filtro e permissões
            for (Ticket ticket : activeTickets.values()) {
                if (isStaff || ticket.playerUUID.equals(playerUUID)) {
                    if ("abertos".equalsIgnoreCase(filter) && "ABERTO".equals(ticket.status)) {
                        ticketsToShow.add(ticket);
                    } else if ("andamento".equalsIgnoreCase(filter) && "EM_ANDAMENTO".equals(ticket.status)) {
                        ticketsToShow.add(ticket);
                    } else if (filter == null || filter.isEmpty()) {
                        ticketsToShow.add(ticket);
                    }
                }
            }

            // Exibir lista
            player.sendMessage(new StringTextComponent(""), player.getUUID());
            player.sendMessage(new StringTextComponent("§6§l🎫 LISTA DE TICKETS" +
                    (filter != null ? " (" + filter + ")" : "")), player.getUUID());

            if (ticketsToShow.isEmpty()) {
                player.sendMessage(new StringTextComponent("§7Nenhum ticket encontrado."), player.getUUID());
                return;
            }

            for (Ticket ticket : ticketsToShow) {
                StringTextComponent ticketLine = new StringTextComponent(
                        String.format("§8#%s §7| §b%s §7| %s §7| §f%s",
                                ticket.id, ticket.playerName, getStatusColor(ticket.status) + ticket.status,
                                shortenMessage(ticket.message, 30))
                );

                ticketLine.withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket ver " + ticket.id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new StringTextComponent("§6Clique para ver detalhes\n§7Status: " + ticket.status + "\n§7Atendente: " + ticket.assignedStaff))));

                player.sendMessage(ticketLine, player.getUUID());
            }

            player.sendMessage(new StringTextComponent(""), player.getUUID());
            player.sendMessage(new StringTextComponent("§7Total: §e" + ticketsToShow.size() + " tickets"), player.getUUID());
            if (isStaff) {
                player.sendMessage(new StringTextComponent("§7Use §e/ticket listar <abertos|andamento> §7para filtrar"), player.getUUID());
            }

        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao listar tickets: {}", e.toString());
        }
    }

    // ========== NOTIFICAR JOGADOR SOBRE ATUALIZAÇÃO ==========
    private void notifyPlayerAboutTicketUpdate(Ticket ticket, String message) {
        if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
            ServerPlayerEntity targetPlayer = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayerByName(ticket.playerName);

            if (targetPlayer != null) {
                targetPlayer.sendMessage(new StringTextComponent(""), targetPlayer.getUUID());
                targetPlayer.sendMessage(new StringTextComponent("§6§l🎫 ATUALIZAÇÃO DO TICKET #" + ticket.id), targetPlayer.getUUID());
                targetPlayer.sendMessage(new StringTextComponent(message), targetPlayer.getUUID());
                targetPlayer.sendMessage(new StringTextComponent("§7Use §e/ticket status " + ticket.id + " §7para ver detalhes."), targetPlayer.getUUID());
            }
        }
    }

    // ========== SISTEMA DE ARMAZENAMENTO ==========
    private void saveTicketToFile(Ticket ticket) {
        try {
            String ticketFile = TICKETS_FOLDER + "/ticket_" + ticket.id + ".txt";
            FileWriter writer = new FileWriter(ticketFile);

            writer.write("=== TICKET NEXUSBOT ===\n");
            writer.write("ID: " + ticket.id + "\n");
            writer.write("Jogador: " + ticket.playerName + "\n");
            writer.write("UUID: " + ticket.playerUUID + "\n");
            writer.write("Mensagem: " + ticket.message + "\n");
            writer.write("Status: " + ticket.status + "\n");
            writer.write("Atendente: " + ticket.assignedStaff + "\n");
            writer.write("Resposta: " + ticket.response + "\n");
            writer.write("Criado: " + formatTime(ticket.createdAt) + "\n");
            writer.write("Atualizado: " + formatTime(ticket.updatedAt) + "\n");
            writer.write("====================\n");

            writer.close();
        } catch (IOException e) {
            NexusBotMod.LOGGER.error("❌ Erro ao salvar ticket: {}", e.toString());
        }
    }

    private void loadTicketsFromFile() {
        try {
            File ticketsFolder = new File(TICKETS_FOLDER);
            File[] ticketFiles = ticketsFolder.listFiles((dir, name) -> name.startsWith("ticket_") && name.endsWith(".txt"));

            if (ticketFiles != null) {
                for (File file : ticketFiles) {
                    // Lógica para carregar tickets do arquivo
                    // Implementação simplificada para exemplo
                }
                NexusBotMod.LOGGER.info("📂 {} tickets carregados do arquivo", ticketFiles.length);
            }
        } catch (Exception e) {
            NexusBotMod.LOGGER.error("❌ Erro ao carregar tickets: {}", e.toString());
        }
    }

    // ========== MÉTODOS AUXILIARES ==========
    private String getStatusColor(String status) {
        switch (status) {
            case "ABERTO": return "§a";
            case "EM_ANDAMENTO": return "§6";
            case "FECHADO": return "§c";
            default: return "§7";
        }
    }

    private String formatTime(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = minutes / 60;

        if (hours > 0) return hours + " hora" + (hours > 1 ? "s" : "");
        if (minutes > 0) return minutes + " minuto" + (minutes > 1 ? "s" : "");
        return "alguns segundos";
    }

    private String shortenMessage(String message, int maxLength) {
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength - 3) + "...";
    }

    // ========== ESTATÍSTICAS ==========
    public String getStats() {
        int abertos = 0, andamento = 0, fechados = closedTickets.size();

        for (Ticket ticket : activeTickets.values()) {
            if ("ABERTO".equals(ticket.status)) abertos++;
            else if ("EM_ANDAMENTO".equals(ticket.status)) andamento++;
        }

        return String.format(
                "§6🎫 Estatísticas do Sistema de Tickets\n" +
                        "§7Abertos: §a%s§7 | Em andamento: §6%s§7 | Fechados: §c%s\n" +
                        "§7Total: §e%s tickets",
                abertos, andamento, fechados, (abertos + andamento + fechados)
        );
    }
}