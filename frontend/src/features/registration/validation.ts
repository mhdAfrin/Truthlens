export type Fields = { name: string; email: string; password: string };
export function validateRegistration(f: Fields) {
  const e: Partial<Fields> = {};
  if (!f.name.trim()) e.name = "Please enter your name.";
  else if (f.name.trim().length < 2 || f.name.trim().length > 100)
    e.name = "Use between 2 and 100 characters.";
  if (
    !/^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/.test(
      f.email.trim(),
    ) ||
    f.email.trim().length > 254
  )
    e.email = "Enter a valid email, like name@example.com.";
  if (
    f.password.length < 8 ||
    new TextEncoder().encode(f.password).length > 72 ||
    !/[a-zA-Z]/.test(f.password) ||
    !/[0-9]/.test(f.password)
  )
    e.password = "Use at least 8 characters, including a letter and a number (maximum 72 bytes).";
  return e;
}
