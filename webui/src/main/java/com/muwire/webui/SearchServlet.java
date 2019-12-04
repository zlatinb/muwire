package com.muwire.webui;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.search.UIResultEvent;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class SearchServlet extends HttpServlet {
    
    private SearchManager searchManager;
    private ConnectionCounter connectionCounter;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String search = req.getParameter("search");
        searchManager.newSearch(search);
        resp.sendRedirect("/MuWire/Home.jsp");
    }
    
    

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String section = req.getParameter("section");
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        if (section.equals("groupBySender")) {
            sb.append("<Searches>");
            for (SearchResults results : searchManager.getResults().values()) {
                sb.append("<Search>");
                sb.append("<uuid>").append(results.getUUID()).append("</uuid>");
                sb.append("<Query>").append(results.getSearch()).append("</Query>");
                Map<Persona, Set<UIResultEvent>> bySender = results.getBySender();
                sb.append("<ResultsBySender>");
                bySender.forEach((sender, resultsFromSender) -> {
                    sb.append("<ResultsFromSender>");
                    sb.append("<Sender>");
                    sb.append(sender.getHumanReadableName());
                    sb.append("</Sender>");
                    resultsFromSender.forEach(result -> {
                        sb.append("<Result>");
                        sb.append("<Name>");
                        sb.append(result.getName());
                        sb.append("</Name>");
                        sb.append("<Size>");
                        sb.append(DataHelper.formatSize2Decimal(result.getSize(), false)).append("B");
                        sb.append("</Size>");
                        String infohash = Base64.encode(result.getInfohash().getRoot());
                        sb.append("<InfoHash>");
                        sb.append(infohash);
                        sb.append("</InfoHash>");
                        sb.append("</Result>");
                    });
                    sb.append("</ResultsFromSender>");
                });
                sb.append("</ResultsBySender>");
                sb.append("</Search>");
            }
            sb.append("</Searches>");  
        } else if (section.equals("groupByFile")) {
            sb.append("<Searches>");
            for (SearchResults results : searchManager.getResults().values()) {     
                sb.append("<Search>");
                sb.append("<uuid>").append(results.getUUID()).append("</uuid>");
                sb.append("<Query>").append(results.getSearch()).append("</Query>");
                Map<InfoHash, Set<UIResultEvent>> byInfohash = results.getByInfoHash();
                sb.append("<ResultsByFile>");
                byInfohash.forEach((infoHash, resultSet) -> {
                    sb.append("<ResultsForFile>");
                    UIResultEvent first = resultSet.iterator().next();
                    sb.append("<InfoHash>").append(Base64.encode(infoHash.getRoot())).append("</InfoHash>");
                    sb.append("<Name>").append(first.getName()).append("</Name>");
                    sb.append("<Size>").append(DataHelper.formatSize2Decimal(first.getSize(), false)).append("B").append("</Size>");
                    resultSet.forEach(result -> {
                        sb.append("<Result>");
                        sb.append("<Sender>").append(result.getSender().getHumanReadableName()).append("</Sender>");
                        sb.append("</Result>");
                    });
                    sb.append("</ResultsForFile>");
                });
                sb.append("</ResultsByFile>");
                sb.append("</Search>");
            }
            sb.append("</Searches>");
        } else if (section.equals("connectionsCount")) {
            sb.append("<Connections>");
            sb.append(connectionCounter.getConnections());
            sb.append("</Connections>");
        }
        resp.setContentType("text/xml");
        resp.getWriter().write(sb.toString());
        resp.flushBuffer();
    }



    @Override
    public void init(ServletConfig config) throws ServletException {
        searchManager = (SearchManager) config.getServletContext().getAttribute("searchManager");
        connectionCounter = (ConnectionCounter) config.getServletContext().getAttribute("connectionCounter");
    }

}
