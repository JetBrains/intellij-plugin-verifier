//$( "#tabs" ).tabs();
$(".plugin").accordion({active: false, collapsible: true, heightStyle: 'content'});
$(".update").accordion({active: false, collapsible: true, heightStyle: 'content'});

$(".detailsLink").click(function () {
    var longDiv = $(this).parent().find(".longDescription");

    if (longDiv.css('display') != 'block') {
        longDiv.css('display', 'block')
    }
    else {
        longDiv.css('display', 'none')
    }

    return false
});

$(".updateHasProblems .uMarker").attr('title', "Problems found");
$(".excluded .uMarker").attr('title', "Excluded");

$(".pluginHasProblem .pMarker").attr('title', "Problems found");
$(".pluginOk .pMarker").attr('title', "Excluded");