<%@ page import="org.jetbrains.plugins.verifier.service.status.ServerStatus" %>
<!doctype html>
<html>
<head>
    <title>Status</title>
</head>

<body>

<div id="parameters">
    <h2>Application parameters:</h2>
    <ul>
        <g:each var="c" in="${ServerStatus.INSTANCE.appProperties()}">
            <li>
                ${c.first} = ${c.second}
            </li>
        </g:each>
    </ul>

    <h2>Status:</h2>
    <ul>
        <g:each var="c" in="${ServerStatus.INSTANCE.parameters()}">
            <li>
                ${c.first} - ${c.second}
            </li>
        </g:each>
    </ul>

    <h2>Available IDEs:</h2>
    <ul>
        <g:each var="c" in="${ServerStatus.INSTANCE.ideFiles()}">
            <li>
                ${c.asString()}
            </li>
        </g:each>
    </ul>

    <h2>Currently running tasks:</h2>
    <ul>
        <g:each var="c" in="${ServerStatus.INSTANCE.getRunningTasks()}">
            <li>
                ${c}
            </li>
        </g:each>
    </ul>

    <h2>Release versions:</h2>
    <ul>
        <g:each var="v" in="${ServerStatus.INSTANCE.releaseVersions()}">
            <li>
                Trunk #${v.first} -> ${v.second.asString()}
            </li>
        </g:each>
    </ul>

</div>

</body>
</html>
