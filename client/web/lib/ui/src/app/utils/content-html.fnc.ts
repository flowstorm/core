export const getContentAsHtml = (content: string): HTMLElement => {
    const tmp = document.createElement('DIV');
    tmp.innerHTML = content;
    return tmp;
};

