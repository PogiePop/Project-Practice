import dayjs from "dayjs";

export function formatDateYYYYMMDD(dateVal) {
    if(!dateVal) return '-';
    const [start, end] = dateVal.split('~');
    const fStart = dayjs(start).format('YYYY-MM-DD');
    const fEnd = dayjs(end).format('YYYY-MM-DD');
    return `${fStart} ~ ${fEnd}`;
}

export function formatDateYYYYMMDDHHMMSS(dateVal) {
    if(!dateVal) return '-';
    const [start, end] = dateVal.split('~');
    const fStart = dayjs(start).format('YYYY-MM-DD HH:MM:SS');
    const fEnd = dayjs(end).format('YYYY-MM-DD HH:MM:SS');
    return `${fStart} ~ ${fEnd}`;
}