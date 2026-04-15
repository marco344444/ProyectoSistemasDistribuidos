import type { FormEvent } from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { storage } from '../services/storage';

type Tab = 'login' | 'register';

export function useAuthController() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('login');
  const [message, setMessage] = useState('');
  const [isError, setIsError] = useState(false);
  const [loading, setLoading] = useState(false);

  const [loginEmail, setLoginEmail] = useState('admin@imageproc.com');
  const [loginPassword, setLoginPassword] = useState('admin123');

  const [nombres, setNombres] = useState('');
  const [apellidos, setApellidos] = useState('');
  const [cedula, setCedula] = useState('');
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');

  const onLogin = async (ev: FormEvent) => {
    ev.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      const data = await api.login(loginEmail.trim(), loginPassword.trim());
      storage.setToken(data.token);
      storage.setUser(data.usuario || loginEmail.trim());
      navigate('/upload');
    } catch (error) {
      setIsError(true);
      setMessage((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const onRegister = async (ev: FormEvent) => {
    ev.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      const response = await api.register({
        nombres: nombres.trim(),
        apellidos: apellidos.trim(),
        cedula: cedula.trim(),
        correo: correo.trim().toLowerCase(),
        password: password.trim(),
      });
      setIsError(false);
      setMessage(response.mensaje || 'Registro exitoso');
      setLoginEmail(correo.trim().toLowerCase());
      setLoginPassword('');
      setTab('login');
    } catch (error) {
      setIsError(true);
      setMessage((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return {
    tab,
    setTab,
    message,
    isError,
    loading,
    loginEmail,
    setLoginEmail,
    loginPassword,
    setLoginPassword,
    nombres,
    setNombres,
    apellidos,
    setApellidos,
    cedula,
    setCedula,
    correo,
    setCorreo,
    password,
    setPassword,
    onLogin,
    onRegister,
  };
}
