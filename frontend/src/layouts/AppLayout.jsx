import { Outlet } from 'react-router-dom';
import TopNavbar from '../components/TopNavbar';
import Sidebar from '../components/Sidebar';

export default function AppLayout() {
  return (
    <div className="app-shell">
      <TopNavbar />
      <div className="app-body">
        <Sidebar />
        <main className="app-main">
          <div className="app-content-frame">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
