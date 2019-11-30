package com.muwire.webui;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SearchServlet extends HttpServlet {
    
    private SearchManager searchManager;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String search = req.getParameter("search");
        searchManager.newSearch(search);
        resp.sendRedirect("/MuWire/Home.jsp");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        searchManager = (SearchManager) config.getServletContext().getAttribute("searchManager");
    }

}
