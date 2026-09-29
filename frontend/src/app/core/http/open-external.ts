/**
 * Opens a URL that came from data (teacher link resources, file URLs) safely:
 *  - only http(s): a stored `javascript:` or `data:` URL would otherwise run in this app's origin
 *    with the viewer's session (stored XSS);
 *  - `noopener,noreferrer`: the opened page gets no handle on this tab (reverse tabnabbing) and no
 *    Referer.
 * Returns false when the URL was refused.
 */
export function openExternal(raw: string | null | undefined): boolean {
  if (!raw) {
    return false;
  }
  let url: URL;
  try {
    url = new URL(raw, window.location.origin);
  } catch {
    return false;
  }
  if (url.protocol !== 'https:' && url.protocol !== 'http:') {
    return false;
  }
  window.open(url.href, '_blank', 'noopener,noreferrer');
  return true;
}
