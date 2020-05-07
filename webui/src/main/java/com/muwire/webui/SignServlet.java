package com.muwire.webui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;

import net.i2p.crypto.DSAEngine;
import net.i2p.data.Base64;
import net.i2p.data.Signature;

public class SignServlet extends HttpServlet {

    private Core core;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String text = req.getParameter("text");
        if (text == null) {
            resp.sendError(503, "Nothing to sign?");
            return;
        }
        
        byte [] payload = text.getBytes(StandardCharsets.UTF_8);
        Signature sig = DSAEngine.getInstance().sign(payload, core.getSpk());
        
        String response = Base64.encode(sig.getData());
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append("<Signed>").append(response).append("</Signed>");
        
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-store, max-age=0, no-cache, must-revalidate");
        byte[] out = sb.toString().getBytes("UTF-8");
        resp.setContentLength(out.length);
        resp.getOutputStream().write(out);
    }
}
