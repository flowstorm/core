export const injectCss = () => {
    const botUiScript = document.querySelector('script[data-bot-ui-resource]');
    if (botUiScript) {
        const scr = (<HTMLScriptElement>botUiScript).src;
        const scriptUri = scr.substring(0, scr.lastIndexOf("/") + 1);
        const styleUri = `${scriptUri}app.css`;
        const styles = document.querySelectorAll(`link[rel="stylesheet"]`);
        let styleAdded = false;
        styles.forEach(style => {
            if ((<HTMLLinkElement>style).href.indexOf(styleUri) === 0) {
                styleAdded = true;
                return;
            }
        });
        if (!styleAdded) {
            const head = document.head;
            const link = document.createElement("link");
            link.type = "text/css";
            link.rel = "stylesheet";
            link.href = styleUri;
            head.appendChild(link);
        }
    }
}
