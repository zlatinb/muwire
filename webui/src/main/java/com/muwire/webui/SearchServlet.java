package com.muwire.webui;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Persona;
import com.muwire.core.search.UIResultEvent;

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
        if (section.equals("activeSearches")) {
            sb.append("<Searches>");
            for (SearchResults results : searchManager.getResults().values()) {
                sb.append("<Search>");
                sb.append("<uuid>").append(results.getUUID()).append("</uuid>");
                sb.append("<Query>").append(results.getSearch()).append("</Query>");
                Map<Persona, Set<UIResultEvent>> bySender = results.getBySender();
                sb.append("<Senders>").append(bySender.size()).append("</Senders>");
                int total = 0;
                for (Set<UIResultEvent> s : bySender.values()) {
                    total += s.size();
                }
                sb.append("<Results>").append(total).append("</Results>");
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
