import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  return (
    <header className="navbar">
      <div className="navbar-brand">JobMatch</div>
      <div className="navbar-user">
        <span>
          {user.fullName} <span className="badge">{user.role.replace('_', ' ')}</span>
        </span>
        <button
          className="link-button"
          onClick={() => {
            logout();
            navigate('/login');
          }}
        >
          Log out
        </button>
      </div>
    </header>
  );
}
