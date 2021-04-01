export const debounce = (func: Function, timeout = 100) => {
    let timer;
    return function(event) {
        if (timer) {
            clearTimeout(timer);
        }
        timer = setTimeout(func, timeout, event);
    };
}
