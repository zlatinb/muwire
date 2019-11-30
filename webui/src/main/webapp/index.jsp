<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
    <head>
        <title>MuWire</title>
    </head>
    <body>
    	<jsp:useBean id="mwBean" class="com.muwire.webui.MuWireBean"/>
    	<c:set var="mwClient" scope="application" value="${mwBean.getMwClient()}"/>
    	<c:if test = "${mwClient.needsMWInit()}">
	        <p>MW needs initializing</p>
	    </c:if>
	    <c:if test = "${!mwClient.needsMWInit()}">
	    	<p>MW doesn't need initing</p>
	  	</c:if>
    </body>
</html>
