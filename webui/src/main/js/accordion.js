var openAccordion = 0;

function initAccordion() {
  var acc = document.getElementsByClassName("accordion");
  var i;

  for (i = 0; i < acc.length; i++) {
    acc[i].addEventListener("click", function() {
      this.classList.toggle("active");
      var panel = this.nextElementSibling;
      if (panel.style.maxHeight) {
        panel.style.maxHeight = null;
      } else {
        panel.style.maxHeight = panel.scrollHeight + "px";
      } 
    });
  }

  if (openAccordion > 0) {
      acc[openAccordion - 1].classList.add("active");
      var panel = acc[openAccordion - 1].nextElementSibling;
      panel.style.maxHeight = panel.scrollHeight + "px";
  }
}


document.addEventListener("DOMContentLoaded", function() {
   initAccordion();
}, true);
